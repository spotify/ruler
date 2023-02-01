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

import com.spotify.ruler.common.dependency.ArtifactResult
import com.spotify.ruler.common.dependency.DependencyEntry
import com.spotify.ruler.common.dependency.DependencyParser
import com.spotify.ruler.common.models.AppInfo
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import java.io.File

/** Responsible for parsing and extracting entries from dependencies. */
class EntryParser {

    /** Parses and returns the list of entries contained in all dependencies of the given [project]. */
    fun parse(project: Project, appInfo: AppInfo): List<DependencyEntry> {
        val configuration =
            project.configurations.getByName("${appInfo.variantName}RuntimeClasspath")
        val entries = mutableListOf<ArtifactResult>()
        val parser = DependencyParser()
        listOf("android-classes").forEach { artifactType ->
            entries += parseFile(configuration, artifactType, true)
        }

        listOf("android-res", "android-assets", "android-jni").forEach { artifactType ->
            entries += parseFile(configuration, artifactType, false)
        }

        return parser.parse(entries)
    }

    private fun getArtifactView(
        configuration: Configuration,
        artifactType: String
    ) = configuration.incoming.artifactView { viewConfiguration ->
        viewConfiguration.attributes { attributeContainer ->
            attributeContainer.attribute(
                Attribute.of("artifactType", String::class.java),
                artifactType
            )
        }
    }.artifacts.artifacts

    private fun parseFile(
        configuration: Configuration,
        artifactType: String,
        isJar: Boolean
    ) = getArtifactView(configuration, artifactType).flatMap { artifactResult ->
        val artifactFiles = artifactResult.file.walkTopDown().filter(File::isFile)
        if (isJar) {
            artifactFiles.map { artifactFile ->
                ArtifactResult.JarArtifact(artifactFile, getComponentIdentifier(artifactResult))
            }
        } else {
            artifactFiles.map { artifactFile ->
                ArtifactResult.DefaultArtifact(
                    artifactFile,
                    artifactResult.file,
                    getComponentIdentifier(artifactResult)
                )
            }
        }
    }

    private fun getComponentIdentifier(artifactResult: ResolvedArtifactResult): String {
        return artifactResult.id.componentIdentifier.displayName
    }
}
