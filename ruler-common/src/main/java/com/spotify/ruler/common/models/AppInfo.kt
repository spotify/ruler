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

import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable

/** General info about an app. */
@KSerializable
data class AppInfo(val variantName: String, val applicationId: String, val versionName: String) :
    Serializable {
    companion object {
        const val serialVersionUID = 1L
    }
}
