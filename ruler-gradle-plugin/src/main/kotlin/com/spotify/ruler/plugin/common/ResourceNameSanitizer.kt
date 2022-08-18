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

package com.spotify.ruler.plugin.common

import java.io.File
import java.io.FileReader
import java.io.Reader
import java.io.StringReader

/** Responsible for sanitizing resource file names (necessary for DexGuard compatibility). */
class ResourceNameSanitizer {
    private val resourceNameMapping = mutableMapOf<String, String>()

    constructor(mappingFile: File?) { mappingFile?.let { initialize(FileReader(it)) } }
    constructor(mapping: String) { initialize(StringReader(mapping)) }

    /** Sanitizes a given [resourceName], which includes deobfuscation (if applicable). */
    fun sanitize(resourceName: String): String {
        return resourceNameMapping[resourceName] ?: resourceName // /res/raw/dVo.xml -> /res/drawable/foo.xml
    }

    /** Initializes the [resourceNameMapping] based on the mapping file read by the given [reader]. */
    private fun initialize(reader: Reader) = reader.forEachLine { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("res/")) {
            return@forEachLine // We're only interested in resource file name mappings for now
        }

        // Parse resource name mapping (res/anim/foo.xml -> [res/raw/a.xml])
        val split = line.split(" -> ")
        if (split.size != 2) {
            return@forEachLine // Couldn't parse line, ignore for now
        }
        val clearName = split[0]
        val obfuscatedName = split[1].removeSurrounding("[", "]")

        resourceNameMapping["/$obfuscatedName"] = "/$clearName"
    }
}
