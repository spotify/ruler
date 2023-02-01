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

package com.spotify.ruler.common.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.Serializable

/** Specification of a device for which APKs can be generated */
@kotlinx.serialization.Serializable(with = DeviceSpec.Serializer::class)
data class DeviceSpec(val abi: String, val locale: String, val screenDensity: Int, val sdkVersion: Int) : Serializable {
    companion object {
        const val serialVersionUID = 1L
    }

    @SerialName("DeviceSpec")
    @kotlinx.serialization.Serializable
    data class DeviceSpecSurrogate(
        val supportedAbis: List<String>,
        val supportedLocales: List<String>,
        val screenDensity: Int,
        val sdkVersion: Int
    )

    /** Serializer which transform a [DeviceSpec] object to/from a JSON format compatible with bundletool. */
    object Serializer : KSerializer<DeviceSpec> {
        override val descriptor = DeviceSpecSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: DeviceSpec) {
            val surrogate = DeviceSpecSurrogate(
                supportedAbis = listOf(value.abi),
                supportedLocales = listOf(value.locale),
                screenDensity = value.screenDensity,
                sdkVersion = value.sdkVersion
            )
            encoder.encodeSerializableValue(DeviceSpecSurrogate.serializer(), surrogate)
        }

        override fun deserialize(decoder: Decoder): DeviceSpec {
            val surrogate = decoder.decodeSerializableValue(DeviceSpecSurrogate.serializer())
            return DeviceSpec(
                abi = surrogate.supportedAbis.single(),
                locale = surrogate.supportedLocales.single(),
                screenDensity = surrogate.screenDensity,
                sdkVersion = surrogate.sdkVersion
            )
        }
    }
}
