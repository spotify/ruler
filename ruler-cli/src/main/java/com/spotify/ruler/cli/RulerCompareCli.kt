package com.spotify.ruler.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.spotify.ruler.common.BaseCompareTask
import com.spotify.ruler.common.models.AabConfig
import com.spotify.ruler.common.models.DeviceSpec
import com.spotify.ruler.common.models.RulerCompareConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

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
class RulerCompareCli : CliktCommand(), BaseCompareTask {
    private val rulerConfigJson by option().file().required()
    private val reportDir by option().file(canBeDir = true).required()

//    private val baseAabFile by option().file().required()
//    private val headAabFile by option().file().required()
//    private val reportDir by option().file(canBeDir = true).required()
//    private val mappingFile: File? by option().file()
//    private val resourceMappingFile: File? by option().file()

    override fun config() = config

    private val config: RulerCompareConfig by lazy {
        val json = Json.decodeFromStream<JsonCompareConfig>(rulerConfigJson.inputStream())
        RulerCompareConfig(
            projectPath = json.projectPath,
            headAab = mapJsonAabConfig(json.head),
            baseAab = mapJsonAabConfig(json.base),
            deviceSpec = json.deviceSpec,
            reportDir = reportDir
        )
    }

    override fun run() {
        super.run()
    }
}

@Serializable
data class JsonCompareConfig(
    val projectPath: String,
    val head: JsonAabConfig,
    val base: JsonAabConfig,
    val deviceSpec: DeviceSpec
)

@Serializable
data class JsonAabConfig(
    val aabPath: String,
    val proguardMap: String,
    val resourceMap: String
)

fun mapJsonAabConfig(json: JsonAabConfig) = AabConfig(
    aab = File(json.aabPath),
    proguardMap = File(json.proguardMap),
    resourceMap = File(json.resourceMap)
)

fun main(args: Array<String>) = RulerCompareCli().main(args)
