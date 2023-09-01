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
import com.spotify.ruler.common.bloaty.Bloaty
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipFile

/** Responsible for parsing and extracting entries from APK files. */
class ApkParser(private val unstrippedNativeLibraryPaths: List<File> = emptyList(),
                private val logger: Logger = Logger.getLogger("Ruler")
) {

    /** Parses and returns the list of entries contained in the given [apkFile]. */
    fun parse(apkFile: File) : List<ApkEntry> {
        logger.log(Level.INFO, unstrippedNativeLibraryPaths.map { it.path }.joinToString { ", " })
        val sizeCalculator = ApkSizeCalculator.getDefault()
        val downloadSizePerFile = sizeCalculator.getDownloadSizePerFile(apkFile.toPath())
        val installSizePerFile = sizeCalculator.getRawSizePerFile(apkFile.toPath())

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
                            Bloaty.parseNativeLibraryEntry(
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

    /** Checks if a certain [entryName] represents a DEX entry. */
    private fun isDexEntry(entryName: String): Boolean {
        return entryName.endsWith(".dex", ignoreCase = true)
    }

    /** Checks if a certain [entryName] represents a native library entry. */
    private fun isNativeLibraryEntry(entryName: String): Boolean {
        return entryName.endsWith(".so", ignoreCase = true)
    }

    /** Get the file containing the Unstripped file names to properly parse the native library. */
    private fun debugFileForNativeLibrary(entryName: String): File? {
        val entryFileName = File(entryName).nameWithoutExtension
        val result = unstrippedNativeLibraryPaths.find {
            it.name.contains(entryFileName)
        }
        logger.info("Looking for unstripped file for $entryName. Matched: $result")
        return result
    }
}
