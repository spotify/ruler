/*
* Copyright 2023 Spotify AB
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

package com.spotify.ruler.common.dependency

/** Responsible for parsing and extracting entries from dependencies. */
class DependencyParser {

    /** Parses and returns the list of entries contained in all dependencies of the given [project]. */
    fun parse(entries: List<ArtifactResult>): List<DependencyEntry> {
        val result = mutableListOf<DependencyEntry>()

        val jarArtifactParser = JarArtifactParser()
        val defaultArtifactParser = DefaultArtifactParser()
        entries.forEach {
            result += when (it) {
                is ArtifactResult.DefaultArtifact -> {
                    defaultArtifactParser.parseFile(it)
                }
                is ArtifactResult.JarArtifact -> {
                    jarArtifactParser.parseFile(it)
                }
            }
        }
        return result
    }
}
