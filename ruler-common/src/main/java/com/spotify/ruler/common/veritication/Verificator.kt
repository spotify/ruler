/*
* Copyright 2024 Spotify AB
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

package com.spotify.ruler.common.veritication

import com.spotify.ruler.models.AppFile

class Verificator(private val config: VerificationConfig) {

    fun verify(components: List<AppFile>) {
        val downloadSize = components.sumOf(AppFile::downloadSize)
        val downloadSizeThreshold = config.downloadSizeThreshold
        if (downloadSize > downloadSizeThreshold) {
            throw SizeExceededException("Download", downloadSize, downloadSizeThreshold)
        }

        val installSize = components.sumOf(AppFile::installSize)
        val installSizeThreshold = config.installSizeThreshold
        if (installSize > installSizeThreshold) {
            throw SizeExceededException("Install", installSize, installSizeThreshold)
        }
    }
}
