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

package com.spotify.ruler.plugin.apk

import com.android.SdkConstants
import com.android.build.gradle.internal.SdkLocator
import com.android.builder.errors.DefaultIssueReporter
import com.android.bundle.Commands
import com.android.bundle.Devices
import com.android.prefs.AndroidLocationsSingleton
import com.android.repository.api.ProgressIndicatorAdapter
import com.android.sdklib.repository.AndroidSdkHandler
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.device.DeviceSpecParser
import com.android.utils.StdLogger
import com.spotify.ruler.plugin.models.DeviceSpec
import java.io.File
import java.io.StringReader
import java.nio.file.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Responsible for creating APKs based on provided app bundle (AAB) files.
 *
 * @param rootDir Root directory of the Gradle project, needed to look up the path of certain binaries.
 */
class ApkCreator(private val rootDir: File) {

    /**
     * Creates APKs based on a bundle file using the logic provided by Googles bundletool.
     *
     * @param bundleFile Source AAB file
     * @param deviceSpec Device specification for which the APKs should be created
     * @param targetDir Directory where the APKs should be located. Contents of this directory will be deleted
     * @return Module name to it's APKS
     */
    fun createSplitApks(bundleFile: File, deviceSpec: DeviceSpec, targetDir: File): Map<String, List<File>> {
        targetDir.listFiles()?.forEach(File::deleteRecursively) // Overwrite existing files

        BuildApksCommand.builder()
            .setBundlePath(bundleFile.toPath())
            .setOutputFile(targetDir.toPath())
            .setDeviceSpec(parseDeviceSpec(deviceSpec))
            .setAapt2Command(Aapt2Command.createFromExecutablePath(getAapt2Location()))
            .setOutputFormat(BuildApksCommand.OutputFormat.DIRECTORY)
            .build()
            .execute()

        val apks = hashMapOf<String, ArrayList<File>>()
        Commands.BuildApksResult.parseFrom(targetDir.resolve("toc.pb").readBytes()).variantList.forEach {
            it.apkSetList.forEach { apkSet ->
                apkSet.apkDescriptionList.forEach { apkDesc ->
                    apks.getOrPut(apkSet.moduleMetadata.name) { arrayListOf() }.add(targetDir.resolve(apkDesc.path))
                }
            }
        }
        return apks
    }

    /** Converts the given [deviceSpec] into a format which bundletool understands. */
    private fun parseDeviceSpec(deviceSpec: DeviceSpec): Devices.DeviceSpec {
        val reader = StringReader(Json.encodeToString(deviceSpec))
        return DeviceSpecParser.parseDeviceSpec(reader)
    }

    /** Finds and returns the location of the aapt2 executable. */
    private fun getAapt2Location(): Path {
        val sdkLocation = getAndroidSdkLocation()
        val sdkHandler = AndroidSdkHandler.getInstance(AndroidLocationsSingleton, sdkLocation)
        val progressIndicator = object : ProgressIndicatorAdapter() { /* No progress reporting */ }
        val buildToolInfo = sdkHandler.getLatestBuildTool(progressIndicator, true)
        return buildToolInfo.location.resolve(SdkConstants.FN_AAPT2)
    }

    /** Finds and returns the location of the Android SDK. */
    private fun getAndroidSdkLocation(): Path {
        val logger = StdLogger(StdLogger.Level.WARNING)
        val issueReporter = DefaultIssueReporter(logger)
        return SdkLocator.getSdkDirectory(rootDir, issueReporter).toPath()
    }
}
