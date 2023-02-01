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

package com.spotify.ruler.common.report

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.common.report.HtmlReporter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HtmlReporterTest {
    private val reporter = HtmlReporter()
    private val json = "{ SAMPLE_JSON_DATA }" // Doesn't have to be valid for this test

    @Test
    fun `HTML report is generated`(@TempDir targetDir: File) {
        val reportFile = reporter.generateReport(json, targetDir)

        assertThat(reportFile.exists()).isTrue()
    }

    @Test
    fun `JSON data is embedded into report`(@TempDir targetDir: File) {
        val reportFile = reporter.generateReport(json, targetDir)
        val reportContent = reportFile.readText()

        assertThat(reportContent).contains(json)
    }

    @Test
    fun `Everything is embedded into a single file`(@TempDir targetDir: File) {
        val reportFile = reporter.generateReport(json, targetDir)
        val otherFiles = targetDir.listFiles()?.filter { it != reportFile }

        assertThat(otherFiles).isEmpty()
    }

    @Test
    fun `Existing reports are overwritten`(@TempDir targetDir: File) {
        repeat(2) {
            reporter.generateReport(json, targetDir)
        }
    }
}
