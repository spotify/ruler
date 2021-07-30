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

package com.spotify.ruler.plugin

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Paths

class RulerIntegrationTest {
    private val projectFixture = Paths.get("src", "test", "resources", "project-fixture").toFile()

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectFixture.copyRecursively(projectDir)
    }

    @ParameterizedTest
    @CsvSource(":app:analyzeDebugBundle,debug", ":app:analyzeReleaseBundle,release")
    fun `All variants can be analyzed`(task: String, variant: String) {
        gradlew(task)

        val reportDir = projectDir.resolve("app/build/reports/ruler/$variant")
        assertThat(reportDir.resolve("report.json").exists()).isTrue()
        assertThat(reportDir.resolve("report.html").exists()).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = [":app:analyzeDebugBundle", ":app:analyzeReleaseBundle"])
    fun `Up-to-date checks are working`(task: String) {
        val firstOutcome = gradlew(task).task(task)?.outcome
        assertThat(firstOutcome).isEqualTo(TaskOutcome.SUCCESS)

        val secondOutcome = gradlew(task).task(task)?.outcome
        assertThat(secondOutcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @ParameterizedTest
    @ValueSource(strings = [":app:analyzeDebugBundle", ":app:analyzeReleaseBundle"])
    fun `Bundle analysis fails if device specification is not configured`(task: String) {
        val buildGradle = projectDir.resolve("app/build.gradle")
        buildGradle.writeText(buildGradle.readText().substringBefore("ruler {"))

        gradlew(task, expectFailure = true)
    }

    private fun gradlew(vararg arguments: String, expectFailure: Boolean = false): BuildResult {
         val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*arguments, "--stacktrace")
            .forwardOutput()

        return if (expectFailure) {
            runner.buildAndFail()
        } else {
            runner.build()
        }
    }
}
