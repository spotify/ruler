/*
* Copyright 2023 Spotify AB
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
@file:OptIn(ExperimentalSerializationApi::class)

package com.spotify.ruler.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.spotify.ruler.common.BaseRulerTask
import com.spotify.ruler.common.FEATURE_NAME
import com.spotify.ruler.common.apk.ApkCreator
import com.spotify.ruler.common.apk.InjectedToolApkCreator
import com.spotify.ruler.common.dependency.ArtifactResult
import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.common.dependency.DependencyEntry
import com.spotify.ruler.common.dependency.DependencySanitizer
import com.spotify.ruler.common.dependency.JarArtifactParser
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.models.DeviceSpec
import com.spotify.ruler.common.models.RulerConfig
import com.spotify.ruler.common.sanitizer.ClassNameSanitizer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class RulerCli : CliktCommand(), BaseRulerTask {
    private val logger = Logger.getLogger("Ruler")
    private val dependencyMap by option().file().required()
    private val rulerConfigJson by option().file().required()
    private val apkFile by option().file().required()
    private val reportDir by option().file(canBeDir = true).required()
    private val mappingFile: File? by option().file()
    private val resourceMappingFile: File? by option().file()
    private val aapt2Tool: File? by option().file()

    override fun print(content: String) = echo(content)

    override fun provideMappingFile() = mappingFile

    override fun provideResourceMappingFile(): File? = resourceMappingFile

    override fun rulerConfig(): RulerConfig = config

    private val config: RulerConfig by lazy {
        val json = Json.decodeFromStream<JsonRulerConfig>(rulerConfigJson.inputStream())
        RulerConfig(
            projectPath = json.projectPath,
            apkFilesMap = createApkFile(json.projectPath, json.deviceSpec!!),
            reportDir = reportDir,
            ownershipFile = json.ownershipFile?.let { File(it) },
            appInfo = json.appInfo,
            deviceSpec = json.deviceSpec,
            defaultOwner = json.defaultOwner,
            omitFileBreakdown = json.omitFileBreakdown
        )
    }

    private val dependencies: Map<String, List<DependencyComponent>> by lazy {
        Json.decodeFromStream(dependencyMap.inputStream())
//        val json = Json.decodeFromStream<ModuleMap>(dependencyMap.inputStream())
//        val jarArtifactParser = JarArtifactParser()
//        val jarDependencies = json.jars.distinctBy {
//            it.jar
//        }.flatMap {
//            jarArtifactParser.parseFile(
//                ArtifactResult.JarArtifact(File(it.jar), it.module)
//            )
//        }
//
//        val assets = json.assets.map {
//            DependencyEntry.Default(it.filename, it.module)
//        }
//
//        val resources = json.resources.distinctBy { "${it.module}:${it.filename}" }.map {
//            DependencyEntry.Default(it.filename, it.module)
//        }
//
//        val entries = jarDependencies + assets + resources
//
//        val classNameSanitizer = ClassNameSanitizer(provideMappingFile())
//        val dependencySanitizer = DependencySanitizer(classNameSanitizer)
//        dependencySanitizer.sanitize(entries)
    }

    override fun provideDependencies(): Map<String, List<DependencyComponent>> = dependencies

    override fun run() {
        logger.log(Level.INFO,  """
        ~~~~~ Starting Ruler ~~~~~
        Using Dependency Map: ${dependencyMap.path}
        Using Ruler Config: ${rulerConfigJson.path}
        Using App File: ${apkFile.path}
        Using Proguard Mapping File: ${mappingFile?.path}
        Using Resource Mapping File: ${resourceMappingFile?.path}
        Using AAPT2: ${aapt2Tool?.path}
        Writing reports to: ${reportDir.path}
        """.trimIndent())
        super.run()
    }

    private fun createApkFile(projectPath: String, deviceSpec: DeviceSpec): Map<String, List<File>> {

        val apkCreator = if (aapt2Tool != null) {
            InjectedToolApkCreator(aapt2Tool!!.toPath())
        } else {
            ApkCreator(File(projectPath))
        }

        return if (apkFile.extension == "apk") {
            mapOf(FEATURE_NAME to listOf(apkFile))
        } else {
            apkCreator.createSplitApks(
                apkFile,
                deviceSpec,
                File(projectPath).resolve(File("tmp")).apply {
                    mkdir()
                }
            )
        }
    }

}

@Serializable
data class JsonRulerConfig(
    val projectPath: String,
    val ownershipFile: String? = null,
    val appInfo: AppInfo,
    val deviceSpec: DeviceSpec? = null,
    val defaultOwner: String,
    val omitFileBreakdown: Boolean
)

@Serializable
data class ModuleMap(
    val assets: List<Asset>,
    val jars: List<Jar>,
    val resources: List<Asset>
)

@Serializable
data class Asset(
    val filename: String,
    val module: String
)

@Serializable
data class Jar(
    val jar: String,
    val module: String
)

fun main(args: Array<String>) = RulerCli().main(args)
