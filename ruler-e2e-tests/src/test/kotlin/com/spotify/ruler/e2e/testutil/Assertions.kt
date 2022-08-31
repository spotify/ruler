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

package com.spotify.ruler.e2e.testutil

import com.google.common.truth.Correspondence
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.ResourceType

data class FileMatcher(
    val name: String,
    val type: FileType,
    val owner: String? = null,
    val resourceType: ResourceType? = null
)

object Correspondence {
    fun file() = Correspondence.from(::compareFiles, "matches")

    // Makes it possible to assert files without matching download/install size
    private fun compareFiles(actual: AppFile?, expected: FileMatcher?): Boolean = when {
        actual == null -> expected == null
        expected == null -> false
        else -> actual.name == expected.name &&
                actual.type == expected.type &&
                actual.owner == expected.owner &&
                actual.resourceType == expected.resourceType
    }
}
