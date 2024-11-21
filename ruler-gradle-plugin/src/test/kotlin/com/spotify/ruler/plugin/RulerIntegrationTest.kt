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

class RulerIntegrationTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        ProjectFixture.load(projectDir)
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

        val tasks = gradlew(task, expectFailure = true).tasks
        assertThat(tasks).isEmpty() // We want to fail early
    }

    @ParameterizedTest
    @CsvSource(":app:analyzeDebugBundle,debug", ":app:analyzeReleaseBundle,release")
    fun `Bundle analysis succeeds if no ownership file is configured`(task: String, variant: String) {
        val buildGradle = projectDir.resolve("app/build.gradle")
        buildGradle.writeText(buildGradle.readText().substringBefore("ownershipFile") + "}")

        gradlew(task)

        val reportDir = projectDir.resolve("app/build/reports/ruler/$variant")
        assertThat(reportDir.resolve("report.json").readText()).doesNotContain("owner")
    }

    @ParameterizedTest
    @ValueSource(strings = [":app:analyzeDebugBundle", ":app:analyzeReleaseBundle"])
    fun `Bundle analysis fails if the ownership file is invalid`(task: String) {
        val ownershipYaml = projectDir.resolve("app/ownership.yaml")
        ownershipYaml.writeText("This is not a valid YAML file")

        gradlew(task, expectFailure = true)
    }

    @ParameterizedTest
    @ValueSource(strings = [":app:analyzeDebugBundle", ":app:analyzeReleaseBundle"])
    fun `Bundle analysis fails if the download size threshold is set and breached`(task: String) {
        val buildGradle = projectDir.resolve("app/build.gradle")
        buildGradle.writeText(
            buildGradle.readText()
                .substringBeforeLast("}") + "verification { downloadSizeThreshold = 100L } }"
        )

        gradlew(task, expectFailure = true)
    }

    @ParameterizedTest
    @ValueSource(strings = [":app:analyzeDebugBundle", ":app:analyzeReleaseBundle"])
    fun `Bundle analysis fails if the install size threshold is set and breached`(task: String) {
        val buildGradle = projectDir.resolve("app/build.gradle")
        buildGradle.writeText(
            buildGradle.readText()
                .substringBeforeLast("}") + "verification { installSizeThreshold = 100L } }"
        )

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
