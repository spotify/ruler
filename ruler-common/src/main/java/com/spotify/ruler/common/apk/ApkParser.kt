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

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.android.tools.apk.analyzer.dex.DexFiles
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

public var totalDownloadSize = 0L

/** Responsible for parsing and extracting entries from APK files. */
class ApkParser(private val unstrippedNativeLibraryPaths: List<File> = emptyList()) {

    val bloatyPath: String? by lazy {
        val path = "which bloaty".runCommand(File("."))?.trim()
        if (path.isNullOrEmpty()) {
            println("Could not find Bloaty. Install Bloaty for more information about native libraries.")
            return@lazy null
        }
        println("Bloaty detected at: $path")
        return@lazy path
    }


    /** Parses and returns the list of entries contained in the given [apkFile]. */
    fun parse(apkFile: File): List<ApkEntry> {
        val sizeCalculator = ApkSizeCalculator.getDefault()
        val downloadSizePerFile = sizeCalculator.getDownloadSizePerFile(apkFile.toPath())
        val installSizePerFile = sizeCalculator.getRawSizePerFile(apkFile.toPath())

        if (!apkFile.path.contains("dynamic")) {
            totalDownloadSize += sizeCalculator.getFullApkDownloadSize(apkFile.toPath())
        }
        val apkEntries = mutableListOf<ApkEntry>()
        ZipFile(apkFile).use { zipFile ->
            zipFile.entries().iterator().forEach { zipEntry ->

                val name = "/${zipEntry.name}"
                val downloadSize = downloadSizePerFile.getValue(name)
                val installSize = installSizePerFile.getValue(name)
                apkEntries += when {
                    isDexEntry(name) -> {
                        val bytes = zipFile.getInputStream(zipEntry).readBytes()
                        ApkEntry.Dex(name, downloadSize, installSize, parseDexEntry(bytes))
                    }

                    isNativeLibraryEntry(name) -> {
                        val bytes = zipFile.getInputStream(zipEntry).readBytes()
                        val native = ApkEntry.NativeLibrary(
                            name,
                            downloadSize,
                            installSize,
                            parseNativeLibraryEntry(
                                bytes,
                                debugFileForNativeLibrary(entryName = name)
                            )
                        )
                        native
                    }
                    // When build from bazel resources coming from
                    // kt_android_library rule starts with /lib/res and are not attributed properly.
                    else -> ApkEntry.Default(
                        name.replace("/lib/res/", "/res/"),
                        downloadSize,
                        installSize
                    )
                }
            }
        }
        return apkEntries
    }

    /** Parses a DEX entry (represented by its [bytes]) and returns a list of all contained class entries. */
    private fun parseDexEntry(bytes: ByteArray): List<ApkEntry> {
        val dexFile = DexFiles.getDexFile(bytes)
        return dexFile.classes.map { classDef ->
            ApkEntry.Default(classDef.type, classDef.size.toLong(), classDef.size.toLong())
        }
    }

    private fun parseNativeLibraryEntry(bytes: ByteArray, debugFile: File?): List<ApkEntry> {
        println("Native Library Detected")
        if (bloatyPath == null || debugFile == null) {
            println("Could not deobfuscate native library. Make sure that Bloaty is installed and an unstripped library is provided.")
            return emptyList()
        }
        val tmpFile = File.createTempFile("native-lib", ".so").apply {
            writeBytes(bytes)
        }
        val command = "$bloatyPath --debug-file=${debugFile.absolutePath} ${tmpFile.absolutePath} -d compileunits  -n 0 --csv"
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val rows = mutableListOf<ApkEntry.Default>()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val cols = line?.split(",")
            if (cols?.count() == 3) {
                val size = cols.last().toLongOrNull() ?: 1
                val entry = ApkEntry.Default(
                    cols.first().substringAfter("../.."),
                    size,
                    size
                )
                rows.add(entry)
            }
        }
        process.waitFor()
        return rows
    }

    /** Checks if a certain [entryName] represents a DEX entry. */
    private fun isDexEntry(entryName: String): Boolean {
        return entryName.endsWith(".dex", ignoreCase = true)
    }

    /** Checks if a certain [entryName] represents a native library entry. */
    private fun isNativeLibraryEntry(entryName: String): Boolean {
        return entryName.endsWith(".so", ignoreCase = true)
    }

    private fun debugFileForNativeLibrary(entryName: String): File? {
        val entryFileName = File(entryName).nameWithoutExtension
        return unstrippedNativeLibraryPaths.find {
            it.name.contains(entryFileName)
        }
    }

    private fun String.runCommand(workingDir: File): String? {
        return try {
            val parts = this.split("\\s".toRegex())
            ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start().apply {
                    waitFor(60, TimeUnit.MINUTES)
                }.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
