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

import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipEntry

interface ArtifactParser<in T> {
    fun parseFile(artifactResult: T): List<DependencyEntry>
}

/** Plain artifact parser which returns a list of all artifact files. */
class DefaultArtifactParser : ArtifactParser<ArtifactResult.DefaultArtifact> {

    override fun parseFile(artifact: ArtifactResult.DefaultArtifact): List<DependencyEntry> {
        val name =
            artifact.file.absolutePath.removePrefix(artifact.resolvedArtifactFile.absolutePath)
        return listOf(DependencyEntry.Default(name, artifact.component))
    }
}

/** Artifact parser for .class files that returns a list of the class artifact. */
class ClassArtifactParser : ArtifactParser<ArtifactResult.ClassArtifact> {
    override fun parseFile(artifact: ArtifactResult.ClassArtifact): List<DependencyEntry> {
        val name =
            artifact.file.absolutePath.removePrefix(artifact.resolvedArtifactFile.absolutePath)
        return listOf(DependencyEntry.Class(name, artifact.component))
    }
}

/** Artifact parser which parses JAR artifacts and returns the contents of those JAR files. */
class JarArtifactParser : ArtifactParser<ArtifactResult.JarArtifact> {

    override fun parseFile(artifactResult: ArtifactResult.JarArtifact): List<DependencyEntry> {
        val component = artifactResult.component
        val log = component == "com.google.firebase:firebase-crashlytics-ndk:18.3.6-fixed"
        val result = JarFile(artifactResult.file).use { jarFile ->
                jarFile.entries().asSequence().filterNot(JarEntry::isDirectory).map { entry ->
                    if (log) {
                        println(entry.name)
                    }
                    when {
                        isClassEntry(entry.name) -> DependencyEntry.Class(entry.name, component)
                        else -> DependencyEntry.Default(entry.name, component)
                    }
                }.toList()
            }
        if (log) {
            println("Here's the results::::")
            result.forEach {
                println(it)
            }
        }
        return result
    }

    private fun isClassEntry(entryName: String): Boolean {
        return entryName.endsWith(".class", ignoreCase = true)
    }

}

class AarArtifactParser: ArtifactParser<ArtifactResult.AarArtifact> {
    private val jarArtifactParser = JarArtifactParser()
    override fun parseFile(artifactResult: ArtifactResult.AarArtifact): List<DependencyEntry> {
        val component = artifactResult.component

        return AarFile(artifactResult.file).use { jarFile ->
            jarFile.entries().asSequence().filterNot(ZipEntry::isDirectory).map { entry ->
                when {
                    isJarEntry(entry.name) -> jarArtifactParser.parseFile(
                        ArtifactResult.JarArtifact(jarFile.jarFile!!, component)
                    )
                    isSoEntry(entry.name) -> listOf(DependencyEntry.Default(entry.name.removePrefix("jni"), component))
                    else -> emptyList()
                }
            }.toList().flatten()
        }
    }
    private fun isJarEntry(entryName: String): Boolean =
        entryName == "classes.jar"
    private fun isSoEntry(entryName: String): Boolean =
        entryName.endsWith(".so", ignoreCase = true)
}

sealed interface ArtifactResult {
    data class DefaultArtifact(
        val file: File,
        val resolvedArtifactFile: File,
        val component: String
    ) : ArtifactResult

    data class JarArtifact(val file: File, val component: String) : ArtifactResult

    data class AarArtifact(val file: File, val component: String) : ArtifactResult

    data class ClassArtifact(val file: File, val resolvedArtifactFile: File, val component: String) : ArtifactResult
}
