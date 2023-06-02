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

package com.spotify.ruler.models

import kotlinx.serialization.Serializable

/** Analysis report of an app. */
@Serializable
data class AppReport(
    val name: String,
    val version: String,
    val variant: String,
    override val downloadSize: Long,
    override val installSize: Long,
    override val uncompressedSize: Long,
    val components: List<AppComponent>,
    val dynamicFeatures: List<DynamicFeature>,
) : Measurable
