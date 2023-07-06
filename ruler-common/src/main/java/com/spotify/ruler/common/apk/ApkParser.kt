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
import java.io.File
import java.util.zip.ZipFile

/** Responsible for parsing and extracting entries from APK files. */
class ApkParser {

    /** Parses and returns the list of entries contained in the given [apkFile]. */
    fun parse(apkFile: File) : List<ApkEntry> {
        val sizeCalculator = ApkSizeCalculator.getDefault()
        val downloadSizePerFile = sizeCalculator.getDownloadSizePerFile(apkFile.toPath())
        val installSizePerFile = sizeCalculator.getRawSizePerFile(apkFile.toPath())

        val apkEntries = mutableListOf<ApkEntry>()
        ZipFile(apkFile).use { zipFile ->
            zipFile.entries().iterator().forEach { zipEntry ->
                println("Reading Entry: name")
                val name = "/${zipEntry.name}"
                val downloadSize = downloadSizePerFile.getValue(name).toDouble()
                val installSize = installSizePerFile.getValue(name).toDouble()
                println("Reading Entry: $name with size: $downloadSize")
                apkEntries += when {
                    isDexEntry(name) -> {
                        val bytes = zipFile.getInputStream(zipEntry).readBytes()
                        ApkEntry.Dex(name, downloadSize, installSize, parseDexEntry(bytes))
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
            ApkEntry.Default(classDef.type, classDef.size.toDouble(), classDef.size.toDouble())
        }
    }

    /** Checks if a certain [entryName] represents a DEX entry. */
    private fun isDexEntry(entryName: String): Boolean {
        return entryName.endsWith(".dex", ignoreCase = true)
    }
}
