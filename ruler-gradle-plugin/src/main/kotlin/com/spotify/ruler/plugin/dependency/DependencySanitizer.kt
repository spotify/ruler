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

package com.spotify.ruler.plugin.dependency

import com.spotify.ruler.plugin.common.ClassNameSanitizer

/**
 * Responsible for sanitizing dependency entries, so they can be attributed easier.
 *
 * @param classNameSanitizer Used for sanitizing class names
 */
class DependencySanitizer(private val classNameSanitizer: ClassNameSanitizer) {

    /**
     * Sanitizes a list of dependency entries, to ease further processing. Sanitizing means cleaning up entry names and
     * associating entries with their components.
     *
     * @param entries List of raw entries parsed from dependencies
     * @return Map of file names to a list of all components which include this file
     */
    fun sanitize(entries: List<DependencyEntry>): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        entries.map(::sanitizeEntry).forEach { entry ->
            val components = map.getOrPut(entry.name) { ArrayList() }
            components += entry.component
        }
        return map
    }

    /** Cleans the component name and potentially sanitizes the name for a given [entry]. */
    private fun sanitizeEntry(entry: DependencyEntry): DependencyEntry {
        val component = entry.component.removePrefix("project ")
        return when(entry) {
            is DependencyEntry.Class -> {
                val name = classNameSanitizer.sanitize(entry.name)
                DependencyEntry.Class(name, component)
            }
            is DependencyEntry.Default -> entry.copy(component = component)
        }
    }
}
