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

import com.android.tools.proguard.ProguardMap
import java.io.File
import java.io.StringReader

/** Responsible for sanitizing class names. */
class ClassNameSanitizer {
    private val proguardMap = ProguardMap()

    constructor(mappingFile: File?) { mappingFile?.let(proguardMap::readFromFile) }
    constructor(mapping: String) { proguardMap.readFromReader(StringReader(mapping)) }

    /** Sanitizes a given [className], which includes deobfuscation (if applicable). */
    fun sanitize(className: String): String {
        val sanitized = className
            .removeSurrounding("L", ";") // La/b/C; -> a/b/C
            .removeSuffix(".class") // a/b/C.class -> a/b/C
            .replace('/', '.') // a/b/c -> a.b.C
        return proguardMap.getClassName(sanitized) // a.b.C -> foo/bar/Baz
    }
}
