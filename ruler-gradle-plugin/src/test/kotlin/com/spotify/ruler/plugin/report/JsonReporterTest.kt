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

package com.spotify.ruler.plugin.report

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.FileType
import com.spotify.ruler.plugin.dependency.DependencyComponent
import com.spotify.ruler.plugin.models.AppInfo
import com.spotify.ruler.plugin.ownership.OwnershipEntry
import com.spotify.ruler.plugin.ownership.OwnershipInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JsonReporterTest {
    private val reporter = JsonReporter()

    private val appInfo = AppInfo("release", "com.spotify.music", "1.2.3")
    private val components = mapOf(
        DependencyComponent(":app", ComponentType.INTERNAL) to listOf(
            AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200),
            AppFile("/res/layout/activity_main.xml", FileType.RESOURCE, 150, 250),
        ),
        DependencyComponent(":lib", ComponentType.INTERNAL) to listOf(
            AppFile("/assets/license.html", FileType.ASSET, 500, 600),
        ),
    )
    private val features = mapOf(
        "dynamic" to listOf(
            AppFile("com.spotify.DynamicActivity", FileType.CLASS, 200, 300),
            AppFile("/res/layout/activity_dynamic.xml", FileType.RESOURCE, 100, 250),
        ),
    )

    private val ownershipEntries = listOf(OwnershipEntry(":app", "app-team"), OwnershipEntry("dynamic", "dynamic-team"))
    private val ownershipInfo = OwnershipInfo(ownershipEntries, "default-team")

    @Test
    fun `JSON report is generated`(@TempDir targetDir: File) {
        val reportFile = reporter.generateReport(appInfo, components, features, ownershipInfo, targetDir)
        val report = Json.decodeFromString<AppReport>(reportFile.readText())

        val expected = AppReport("com.spotify.music", "1.2.3", "release", 750, 1050, listOf(
            AppComponent(":lib", ComponentType.INTERNAL, 500, 600, listOf(
                AppFile("/assets/license.html", FileType.ASSET, 500, 600, "default-team"),
            ), "default-team"),
            AppComponent(":app", ComponentType.INTERNAL, 250, 450, listOf(
                AppFile("/res/layout/activity_main.xml", FileType.RESOURCE, 150, 250, "app-team"),
                AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200, "app-team"),
            ), "app-team"),
        ), listOf(
            DynamicFeature("dynamic", 300, 550, listOf(
                AppFile("com.spotify.DynamicActivity", FileType.CLASS, 200, 300, "dynamic-team"),
                AppFile("/res/layout/activity_dynamic.xml", FileType.RESOURCE, 100, 250, "dynamic-team"),
            ), "dynamic-team"),
        ))
        assertThat(report).isEqualTo(expected)
    }

    @Test
    fun `Ownership info is omitted from report if it is null`(@TempDir targetDir: File) {
        val reportFile = reporter.generateReport(appInfo, components, features, null, targetDir)
        val reportContent = reportFile.readText()
        assertThat(reportContent).doesNotContain("owner")
    }

    @Test
    fun `Existing reports are overwritten`(@TempDir targetDir: File) {
        repeat(2) {
            reporter.generateReport(appInfo, components, features, ownershipInfo, targetDir)
        }
    }
}
