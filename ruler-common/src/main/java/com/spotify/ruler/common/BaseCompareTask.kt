package com.spotify.ruler.common

import com.android.bundle.Commands
import com.spotify.ruler.common.apk.ApkParser
import com.spotify.ruler.common.apk.ApkSanitizer
import com.spotify.ruler.common.compare.RulerComparer
import com.spotify.ruler.common.models.AabConfig
import com.spotify.ruler.common.models.DeviceSpec
import com.spotify.ruler.common.models.DifferentAppFile
import com.spotify.ruler.common.models.RulerCompareConfig
import com.spotify.ruler.common.sanitizer.ClassNameSanitizer
import com.spotify.ruler.common.sanitizer.ResourceNameSanitizer
import com.spotify.ruler.models.AppFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipFile


interface BaseCompareTask {
    fun config(): RulerCompareConfig

    fun run() {
        val head = createSplitApks(
            config().headAab.aab,
            config().deviceSpec,
            config().reportDir,
            "head"
        )

        val base = createSplitApks(
            config().baseAab.aab,
            config().deviceSpec,
            config().reportDir,
            "base"
        )

        val headAppFiles = getAppFiles(head, config().headAab)
        val baseAppFiles = getAppFiles(base, config().baseAab)
        val diff = RulerComparer().compareBuilds(headAppFiles.flatMap { it.value }, baseAppFiles.flatMap { it.value })
        val json = Json.encodeToString(diff)
        println(json)
    }

    private fun getAppFiles(
        apk: Map<String, List<File>>,
        aabConfig: AabConfig
    ): Map<String, List<AppFile>> {
        val apkParser = ApkParser()
        val classNameSanitizer = ClassNameSanitizer(aabConfig.proguardMap)
        val resourceNameSanitizer = ResourceNameSanitizer(aabConfig.resourceMap)
        val apkSanitizer = ApkSanitizer(classNameSanitizer, resourceNameSanitizer)

        return apk.mapValues { (_, apks) ->
            val entries = apks.flatMap(apkParser::parse)
            apkSanitizer.sanitize(entries)
        }
    }

    fun createSplitApks(
        bundleFile: File,
        deviceSpec: DeviceSpec,
        targetDir: File,
        fileName: String
    ): Map<String, List<File>> {

        val deviceSpecJson = Json.encodeToString(deviceSpec)
        val deviceSpecFile = createTempJsonFile(deviceSpecJson)

        val outputPath = "${targetDir.absolutePath}/$fileName"

        File(outputPath).listFiles()?.forEach(File::deleteRecursively) // Overwrite existing files

        val processBuilder = ProcessBuilder()
            .command(
                "bundletool", "build-apks",
                "--bundle=${bundleFile.absolutePath}",
                "--output=$outputPath/app.apks",
                "--device-spec=${deviceSpecFile.absolutePath}",
            )
        val process = processBuilder.start()
        process.waitFor()

        val command = "unzip $outputPath/app.apks -d $outputPath"
        Runtime.getRuntime().exec(command).waitFor()

        val zip = ZipFile("$outputPath/app.apks")
        val entry = zip.getEntry("toc.pb")
        val result = Commands.BuildApksResult.parseFrom(zip.getInputStream(entry))
        val variant =
            result.variantList.single() // We're targeting one device -> we only expect a single variant
        return variant.apkSetList.associate { apkSet ->
            val moduleName = apkSet.moduleMetadata.name
            val moduleSplits = apkSet.apkDescriptionList.map { File(outputPath, it.path) }
            moduleName to moduleSplits
        }
    }

    private fun createTempJsonFile(jsonContent: String): File {
        val tempFile = File.createTempFile("deviceSpec", ".json")
        tempFile.writeText(jsonContent)
        tempFile.deleteOnExit()
        return tempFile
    }

    // Function to print the table
    fun printTable(data: List<DifferentAppFile>) {
        println("-------------------------------------------------------")
        println("|   Difference   |   Old Size    |   New Size    |  Name")
        println("-------------------------------------------------------")

        for (appFile in data) {
            val name = appFile.name
            val oldSize = formatBytes(appFile.oldSize)
            val newSize = formatBytes(appFile.newSize)
            val difference = formatBytes(appFile.difference)

            // Aligning the columns using String.format
            val formattedString =
                "| %-15s| %15s| %15s| %14s".format(difference, oldSize, newSize, name)
            println(formattedString)
        }

        println("-------------------------------------------------------")
    }

    fun formatBytes(bytes: Long): String {

        val kiloByte = 1024
        val megaByte = kiloByte * kiloByte
        val gigaByte = megaByte * kiloByte

        val absoluteBytes = Math.abs(bytes)

        return when {
            absoluteBytes >= gigaByte -> String.format(
                "%.2f GB",
                absoluteBytes.toFloat() / gigaByte
            )

            absoluteBytes >= megaByte -> String.format(
                "%.2f MB",
                absoluteBytes.toFloat() / megaByte
            )

            absoluteBytes >= kiloByte -> String.format(
                "%.2f KB",
                absoluteBytes.toFloat() / kiloByte
            )

            else -> "$bytes B"
        }
    }
}
