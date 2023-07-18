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
import java.io.IOException
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
                // println("Reading Entry: name")
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
                        ApkEntry.NativeLibrary(
                            name,
                            downloadSize,
                            installSize,
                            parseNativeLibraryEntry(
                                bytes,
                                debugFileForNativeLibrary(entryName = name)
                            )
                        )
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
        val command =
            "$bloatyPath --debug-file=${debugFile.absolutePath} ${tmpFile.absolutePath} -d compileunits  -n 0 --csv"
//        println(command)
//        val result =
//            """../../spotify/client_features/features/popcount/cpp/src/popcount_stack.cpp,64,64
//../../spotify/client_features/features/local_files_esperanto/cpp/src/local_files_esperanto_stack.cpp,60,60
//../../spotify/client_features/features/presence/cpp/src/presence_stack.cpp,60,60
//../../spotify/connect/common/cpp/src/connect_identifier.cpp,60,60
//../../spotify/metadata_delivery/mdata/cpp/src/cache_availability_query_delegate.cpp,60,60
//../../spotify/player_model/cpp/src/command/comeback_command.cpp,60,60
//../../vendor/flac/src/libFLAC/cpu.c,60,60
//../../spotify/client_features/features/social_listening/cpp/src/social_listening_stack.cpp,56,56
//../../spotify/connect/common/cpp/src/http_error_mapper.cpp,56,56
//../../spotify/metadata_delivery/mdata/cpp/src/offline_state_query_delegate.cpp,56,56
//../../spotify/metadata_delivery/metadata/lens/src/lens/playback.cpp,56,56
//../../spotify/player_model/cpp/src/reason_for_moving.cpp,56,56
//../../spotify/content-delivery/bitrate/cpp/src/key_info_visitor.cpp,52,52
//../../spotify/player_model/cpp/src/suppressions.cpp,52,52
//../../spotify/player_model/cpp/src/timer_event.cpp,52,52
//../../spotify/client_features/features/analyzer/cpp/src/analyzer_stack.cpp,48,48
//../../spotify/client_features/features/collection_platform_cosmos/cpp/src/collection_platform_cosmos_stack.cpp,48,48
//../../spotify/client_features/features/facebook/cpp/src/facebook_stack.cpp,48,48
//../../spotify/client_features/features/frecency/cpp/src/frecency_stack.cpp,48,48
//../../spotify/collection_cosmos/cpp/src/collection_cosmos_sources.cpp,48,48
//../../spotify/libs/aqueduct/cpp/src/SystemInfo.cpp,48,48
//../../spotify/recently_played_cosmos/cpp/src/recently_played_cosmos_sources.cpp,48,48
//../../spotify/client_features/features/playlist_offlining/cpp/src/playlist_offlining_stack.cpp,44,44
//../../spotify/crash_diagnostics/cpp/src/abort_on_new_failure.cpp,44,44
//../../spotify/event_sender/jni/src/impl/native_event_sender_jni.cpp,44,44
//../../spotify/player_model/cpp/src/context_player_options.cpp,44,44
//../../spotify/connectivity/connectivity_sdk/components/netstat/cpp/src/netstat_stack.cpp,40,40
//../../vendor/mbedtls/library/platform_util.c,32,32
//""".lines()
        val result = command.runCommand(tmpFile.parentFile)?.lines()

        val rows =  result?.mapNotNull {
            val cols = it.split(",")
            println(cols)
            if (cols.count() != 3) {
                return@mapNotNull null
            }
            ApkEntry.Default(
                cols.first().substringAfter("../.."),
                cols.last().toLong(),
                cols.last().toLong()
            )
        }
        //tmpFile.delete()
        println(rows)
        return rows ?: emptyList()
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
