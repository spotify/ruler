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

package com.spotify.ruler.plugin.apk

/** Single entry of an APK. */
sealed class ApkEntry {
    abstract val name: String
    abstract val downloadSize: Long
    abstract val installSize: Long

    /** Default APK entry. If an entry has no special type, it is considered to be a default entry. */
    data class Default(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
    ) : ApkEntry()

    /** DEX file APK entry containing compiled classes. */
    data class Dex(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
        val classes: List<ApkEntry>,
    ) : ApkEntry()
}
