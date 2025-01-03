/*
* Copyright 2021 Spotify AB
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.spotify.ruler.common.apk

import com.android.SdkConstants
import com.android.bundle.Commands.BuildApksResult
import com.android.bundle.Devices
import com.android.prefs.AndroidLocationsSingleton
import com.android.repository.api.ProgressIndicatorAdapter
import com.android.sdklib.repository.AndroidSdkHandler
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.device.DeviceSpecParser
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import com.spotify.ruler.common.models.DeviceSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipInputStream

const val BUFFER_SIZE = 1024

/**
 * Responsible for creating APKs based on provided app bundle (AAB) files.
 *
 * @param androidSDKDir Root directory of the Gradle project, needed to look up the path of certain binaries.
 */
open class ApkCreator(private val androidSDKDir: File? = null) {

    private val rulerDebugKey = "rulerDebug.keystore"
    private val rulerKeystorePassword = "rulerpassword"
    private val rulerKeyAlias = "rulerdebugkey"

    /**
     * Creates APKs based on a bundle file using the logic provided by Googles bundletool.
     *
     * @param bundleFile Source AAB file
     * @param deviceSpec Device specification for which the APKs should be created
     * @param targetDir Directory where the APKs should be located. Contents of this directory will be deleted
     * @return Map of modules from the AAB file with all the APKs belonging to each module
     */
    fun createSplitApks(
        bundleFile: File,
        deviceSpec: DeviceSpec,
        targetDir: File
    ): Map<String, List<File>> {
        targetDir.listFiles()?.forEach(File::deleteRecursively) // Overwrite existing files

        BuildApksCommand.builder()
            .setBundlePath(bundleFile.toPath())
            .setOutputFile(targetDir.toPath())
            .setDeviceSpec(parseDeviceSpec(deviceSpec))
            .setAapt2Command(Aapt2Command.createFromExecutablePath(getAapt2Location()))
            .setOutputFormat(BuildApksCommand.OutputFormat.DIRECTORY)
            .setSigningConfiguration(getAndroidDebugKey())
            .build()
            .execute()

        return parseSplitApkDirectory(targetDir)
    }

    /** Converts the given [deviceSpec] into a format which bundletool understands. */
    private fun parseDeviceSpec(deviceSpec: DeviceSpec): Devices.DeviceSpec {
        val reader = StringReader(Json.encodeToString(deviceSpec))
        return DeviceSpecParser.parseDeviceSpec(reader)
    }

    /** Finds and returns the location of the aapt2 executable. */
    open fun getAapt2Location(): Path {
        val sdkLocation = (androidSDKDir ?: File(checkNotNull(System.getenv("ANDROID_HOME")) {
            "Missing 'ANDROID_HOME' environment variable"
        }))
        val sdkHandler = AndroidSdkHandler.getInstance(AndroidLocationsSingleton, sdkLocation.toPath())
        val progressIndicator = object : ProgressIndicatorAdapter() { /* No progress reporting */ }
        val buildToolInfo = sdkHandler.getLatestBuildTool(progressIndicator, true)
        return buildToolInfo.location.resolve(SdkConstants.FN_AAPT2)
    }

    /**
     * Gets Ruler debug signing key from resource to sign the split apks.
     * Doing this step makes sure the corresponding /META-INF/BNDLTOOL.SF and *.RSA files are created in the apks.
     **/
    private fun getAndroidDebugKey(): SigningConfiguration {
        val keystoreFile = ApkCreator::class.java.classLoader!!.getResourceAsStream(rulerDebugKey)
            ?: throw java.lang.RuntimeException("Unable to load $rulerDebugKey file.")

        val debugKeyFile = File.createTempFile("debugKey", ".keystore")
        debugKeyFile.deleteOnExit()
        debugKeyFile.outputStream().use { outputStream ->
            keystoreFile.copyTo(outputStream)
        }
        return SigningConfiguration.extractFromKeystore(
            debugKeyFile.toPath(),
            rulerKeyAlias,
            Optional.of(Password.createFromStringValue("pass:$rulerKeystorePassword")),
            Optional.empty()
        )
    }

    companion object {

        /** Name of the feature that contains the main app (without any dynamic feature modules). */
        const val BASE_FEATURE_NAME = "base"
    }
}

class InjectedToolApkCreator(private val aapt2Tool: Path) : ApkCreator() {
    override fun getAapt2Location(): Path = aapt2Tool
}

@Suppress("NestedBlockDepth")
fun unzipFile(zipFile: File, destDirectory: Path) {
    val logger = Logger.getLogger("Ruler")
    val buffer = ByteArray(BUFFER_SIZE)

    // Create a temporary directory
    Files.createDirectories(destDirectory)

    // Create ZipInputStream to read the zip file
    val zipInputStream = ZipInputStream(FileInputStream(zipFile))

    // Loop through each entry in the zip file
    var zipEntry = zipInputStream.nextEntry
    while (zipEntry != null) {
        val newFile = destDirectory.resolve(zipEntry.name)
        logger.log(Level.INFO, "extracting $zipEntry to $newFile")

        // Create necessary directories if they don't exist
        if (zipEntry.isDirectory) {
            Files.createDirectories(newFile)
        } else {
            newFile.toFile().parentFile.mkdirs()
            // Create FileOutputStream to write the file
            FileOutputStream(newFile.toFile()).use { fos ->
                // Read and write the data
                var len = zipInputStream.read(buffer)
                while (len > 0) {
                    fos.write(buffer, 0, len)
                    len = zipInputStream.read(buffer)
                }
            }
        }

        // Move to the next entry in the zip file
        zipEntry = zipInputStream.nextEntry
    }

    // Close the ZipInputStream
    zipInputStream.closeEntry()
    zipInputStream.close()

    println("File successfully unzipped to $destDirectory")
}

fun parseSplitApkDirectory(targetDir: File): Map<String, List<File>> {
    val result = BuildApksResult.parseFrom(targetDir.resolve("toc.pb").readBytes())
    val variant =
        result.variantList.single() // We're targeting one device -> we only expect a single variant

    return variant.apkSetList.associate { apkSet ->
        val moduleName = apkSet.moduleMetadata.name
        val moduleSplits = apkSet.apkDescriptionList.map { targetDir.resolve(it.path) }
        moduleName to moduleSplits
    }
}
