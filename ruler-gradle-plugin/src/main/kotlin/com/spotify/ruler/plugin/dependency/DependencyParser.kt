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

import com.spotify.ruler.plugin.models.AppInfo
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

/** Responsible for parsing and extracting entries from dependencies. */
class DependencyParser {

    /** Parses and returns the list of entries contained in all dependencies of the given [project]. */
    fun parse(project: Project, appInfo: AppInfo): List<DependencyEntry> {
//        val configuration = project.configurations.getByName("${appInfo.variantName}RuntimeClasspath")
        val entries = mutableListOf<DependencyEntry>()
//
//        val jarArtifactParser = JarArtifactParser()
//        listOf("android-classes").forEach { artifactType ->
//            entries += jarArtifactParser.parse(configuration, artifactType)
//        }
//
//        val defaultArtifactParser = DefaultArtifactParser()
//        listOf("android-res", "android-assets", "android-jni").forEach { artifactType ->
//            entries += defaultArtifactParser.parse(configuration, artifactType)
//        }

        return entries
    }

    /** Parses dependency entries for a certain type of artifact. */
    private abstract class ArtifactParser {
        abstract fun parseFile(artifact: File, artifactResult: ResolvedArtifactResult): List<DependencyEntry>

        /** Parses artifacts of a given [configuration] which have the given [artifactType]. */
        fun parse(configuration: Configuration, artifactType: String): List<DependencyEntry> {
            val artifactView = configuration.incoming.artifactView { viewConfiguration ->
                viewConfiguration.attributes { attributeContainer ->
                    attributeContainer.attribute(Attribute.of("artifactType", String::class.java), artifactType)
                }
            }

            return artifactView.artifacts.artifacts.flatMap { artifactResult ->
                val artifactFiles = artifactResult.file.walkTopDown().filter(File::isFile)
                artifactFiles.flatMap { artifactFile -> parseFile(artifactFile, artifactResult) }
            }
        }

        protected fun getComponentIdentifier(artifactResult: ResolvedArtifactResult): String {
            return artifactResult.id.componentIdentifier.displayName
        }
    }

    /** Plain artifact parser which returns a list of all artifact files. */
    private class DefaultArtifactParser : ArtifactParser() {

        override fun parseFile(artifact: File, artifactResult: ResolvedArtifactResult): List<DependencyEntry> {
            val name = artifact.absolutePath.removePrefix(artifactResult.file.absolutePath)
            val component = getComponentIdentifier(artifactResult)
            return listOf(DependencyEntry.Default(name, component))
        }
    }

    /** Artifact parser which parses JAR artifacts and returns the contents of those JAR files. */
    private class JarArtifactParser : ArtifactParser() {

        override fun parseFile(artifact: File, artifactResult: ResolvedArtifactResult): List<DependencyEntry> {
            val component = getComponentIdentifier(artifactResult)
            return JarFile(artifact).use { jarFile ->
                jarFile.entries().asSequence().filterNot(JarEntry::isDirectory).map { entry ->
                    when {
                        isClassEntry(entry.name) -> DependencyEntry.Class(entry.name, component)
                        else -> DependencyEntry.Default(entry.name, component)
                    }
                }.toList()
            }
        }

        private fun isClassEntry(entryName: String): Boolean {
            return entryName.endsWith(".class", ignoreCase = true)
        }
    }
}
