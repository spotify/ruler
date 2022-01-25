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

package com.spotify.ruler.plugin.testutil

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths

/** Responsible for loading the Android project fixture from the test resources. */
object ProjectFixture {

    // Dependencies class declared in the buildSrc folder of the project
    private val dependencies by lazy {
        val buildSrcClasses = Paths.get("..", "buildSrc", "build", "classes", "kotlin", "main")
        val classLoader = URLClassLoader(arrayOf(buildSrcClasses.toUri().toURL()))
        classLoader.loadClass("Dependencies")
    }

    /**
     * Loads the Android project fixture from the test resources and injects all necessary versions.
     *
     * @param targetDir Directory where the project fixture should be copied into
     */
    fun load(targetDir: File) {
        val projectFixture = Paths.get("src", "test", "resources", "project-fixture")
        projectFixture.toFile().copyRecursively(targetDir)

        // Align versions across all test sources
        injectVersions(targetDir.resolve("build.gradle"))
    }

    private fun injectVersions(buildGradle: File) {
        val buildGradleContent = buildGradle.readText()
            .injectVersion("ANDROID_GRADLE_PLUGIN_MARKER", "ANDROID_GRADLE_PLUGIN")
            .injectVersion("RULER_GRADLE_PLUGIN_MARKER", "RULER_GRADLE_PLUGIN")
        buildGradle.writeText(buildGradleContent)
    }

    /**
     * Replaces a given [marker] in the source build.gradle file with the [field] from the common [dependencies]. If
     * there is an environment variable for the given [field], the version from that will be used instead.
     */
    private fun String.injectVersion(marker: String, field: String): String {
        var dependency = dependencies.getField(field).get(dependencies).toString()

        // Override the version of the dependency if an environment variable exists
        val versionOverride = System.getenv("${field}_VERSION")
        if (versionOverride != null) {
            dependency = dependency.replaceAfterLast(':', versionOverride)
        }

        return replace(marker, dependency)
    }
}
