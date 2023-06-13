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

package com.spotify.ruler.common.ownership

import org.yaml.snakeyaml.Yaml
import java.io.File

private const val CODE_POINT_LIMIT = 100 * 1024 * 1024 // 100 MB

/** Responsible for parsing and extracting ownership entries from the ownership file. */
class OwnershipFileParser {


    /**
     * Parses and returns the list of ownership entries contained in the given [ownershipFile].
     *
     * @throws IllegalStateException If the [ownershipFile] could not be parsed.
     */
    fun parse(ownershipFile: File): List<OwnershipEntry> = try {
        val yaml = Yaml()

        val entries: List<Map<String, String>> = ownershipFile.inputStream().use(yaml::load)
        entries.map { entry ->
            OwnershipEntry(entry.getValue("identifier"), entry.getValue("owner"))
        }
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        throw IllegalStateException("Could not parse ownership file", exception)
    }
}
