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
import com.spotify.ruler.models.Insights
import com.spotify.ruler.models.OwnedComponentSize
import com.spotify.ruler.models.ComponentOwner
import com.spotify.ruler.models.OwnershipOverview
import com.spotify.ruler.models.ResourceType
import com.spotify.ruler.models.TypeInsights
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
            AppFile("/res/layout/activity_main.xml", FileType.RESOURCE, 150, 250, resourceType = ResourceType.LAYOUT),
            AppFile("com.spotify.LoginActivity", FileType.CLASS, 50, 100, "login-team"),
        ),
        DependencyComponent(":lib", ComponentType.INTERNAL) to listOf(
            AppFile("/assets/license.html", FileType.ASSET, 500, 600),
        ),
    )
    private val features = mapOf(
        "dynamic" to listOf(
            AppFile("com.spotify.DynamicActivity", FileType.CLASS, 200, 300),
            AppFile(
                "/res/layout/activity_dynamic.xml",
                FileType.RESOURCE,
                100,
                250,
                resourceType = ResourceType.LAYOUT
            ),
        ),
    )

    private val ownershipEntries = listOf(
        OwnershipEntry(":app", "app-team"),
        OwnershipEntry("com.spotify.LoginActivity", "login-team"),
        OwnershipEntry("dynamic", "dynamic-team")
    )
    private val ownershipInfo = OwnershipInfo(ownershipEntries, "default-team")

    @Test
    fun `JSON report is generated`(@TempDir targetDir: File) {
        val reportFile = reporter.generateReport(appInfo, components, features, ownershipInfo, false, targetDir)
        val report = Json.decodeFromString<AppReport>(reportFile.readText())

        val expected = AppReport(
            name = "com.spotify.music",
            version = "1.2.3",
            variant = "release",
            components = listOf(
                AppComponent(
                    name =":lib",
                    type = ComponentType.INTERNAL,
                    downloadSize = 500,
                    installSize = 600,
                    files = listOf(
                        AppFile("/assets/license.html", FileType.ASSET, 500, 600, "default-team"),
                    ),
                    owner = ComponentOwner(name = "default-team", ownedSize = OwnedComponentSize(downloadSize = 500, installSize = 600))
                ),
                AppComponent(
                    name = ":app",
                    type = ComponentType.INTERNAL,
                    downloadSize = 300,
                    installSize = 550,
                    files = listOf(
                        AppFile("/res/layout/activity_main.xml", FileType.RESOURCE, 150, 250, "app-team", ResourceType.LAYOUT),
                        AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200, "app-team"),
                        AppFile("com.spotify.LoginActivity", FileType.CLASS, 50, 100, "login-team"),
                    ),
                    owner = ComponentOwner(name = "app-team", ownedSize = OwnedComponentSize(downloadSize = 250, installSize = 450))
                ),
            ),
            dynamicFeatures = listOf(
                DynamicFeature(
                    name = "dynamic",
                    downloadSize = 300,
                    installSize = 550,
                    files = listOf(
                        AppFile("com.spotify.DynamicActivity", FileType.CLASS, 200, 300, "dynamic-team"),
                        AppFile("/res/layout/activity_dynamic.xml", FileType.RESOURCE, 100, 250, "dynamic-team", ResourceType.LAYOUT),
                    ),
                    owner = ComponentOwner(name = "dynamic-team", ownedSize = OwnedComponentSize(downloadSize = 300, installSize = 550))
                ),
            ),
            insights = Insights(
                appDownloadSize = 800,
                appInstallSize = 1150,
                fileTypeInsights = mapOf(
                    FileType.CLASS to TypeInsights(downloadSize = 150, installSize = 300, count = 2),
                    FileType.RESOURCE to TypeInsights(downloadSize = 150, installSize = 250, count = 1),
                    FileType.ASSET to TypeInsights(downloadSize = 500, installSize = 600, count = 1)
                ),
                componentTypeInsights = mapOf(
                    ComponentType.INTERNAL to TypeInsights(downloadSize = 800, installSize = 1150, count = 2)
                ),
                resourcesTypeInsights = mapOf(
                    ResourceType.LAYOUT to TypeInsights(downloadSize = 150, installSize = 250, count = 1)
                ),
            ),
            ownershipOverview = mapOf(
                "default-team" to OwnershipOverview(
                    totalDownloadSize = 500,
                    totalInstallSize = 600,
                    filesCount = 1,
                    filesFromNotOwnedComponentsDownloadSize = 0,
                    filesFromNotOwnedComponentsInstallSize = 0,
                ),
                "app-team" to OwnershipOverview(
                    totalDownloadSize = 250,
                    totalInstallSize = 450,
                    filesCount = 2,
                    filesFromNotOwnedComponentsDownloadSize = 0,
                    filesFromNotOwnedComponentsInstallSize = 0,
                ),
                "login-team" to OwnershipOverview(
                    totalDownloadSize = 50,
                    totalInstallSize = 100,
                    filesCount = 1,
                    filesFromNotOwnedComponentsDownloadSize = 50,
                    filesFromNotOwnedComponentsInstallSize = 100,
                )
            )
        )
        assertThat(report).isEqualTo(expected)
    }

    @Test
    fun `JSON report is generated without file listing`(@TempDir targetDir: File) {
        val components = mapOf(
            DependencyComponent(":app", ComponentType.INTERNAL) to listOf(
                AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200),
            ),
        )
        val features = mapOf(
            "dynamic" to listOf(
                AppFile("com.spotify.DynamicActivity", FileType.CLASS, 200, 300)
            )
        )
        val reportFile = reporter.generateReport(appInfo, components, features, null, true, targetDir)
        val report = Json.decodeFromString<AppReport>(reportFile.readText())

        val expected = AppReport(
            name = "com.spotify.music",
            version = "1.2.3",
            variant = "release",
            components = listOf(
                AppComponent(
                    name =":app",
                    type = ComponentType.INTERNAL,
                    downloadSize = 100,
                    installSize = 200,
                    files = emptyList(),
                    owner = null
                ),
            ),
            dynamicFeatures = listOf(
                DynamicFeature(
                    name = "dynamic",
                    downloadSize = 200,
                    installSize = 300,
                    files = emptyList(),
                    owner = null
                ),
            ),
            insights = Insights(
                appDownloadSize = 100,
                appInstallSize = 200,
                fileTypeInsights = mapOf(
                    FileType.CLASS to TypeInsights(downloadSize = 100, installSize = 200, count = 1),
                ),
                componentTypeInsights = mapOf(
                    ComponentType.INTERNAL to TypeInsights(downloadSize = 100, installSize = 200, count = 1)
                ),
                resourcesTypeInsights = emptyMap(),
            ),
            ownershipOverview = null
        )

        assertThat(expected).isEqualTo(report)
    }

    @Test
    fun `Ownership info is omitted from report if it is null`(@TempDir targetDir: File) {
        val reportFile = reporter.generateReport(appInfo, components, features, null, false, targetDir)
        val reportContent = reportFile.readText()
        assertThat(reportContent).doesNotContain("\"owner\"")
        assertThat(reportContent).contains("\"ownershipOverview\": null")
    }

    @Test
    fun `Existing reports are overwritten`(@TempDir targetDir: File) {
        repeat(2) {
            reporter.generateReport(appInfo, components, features, ownershipInfo, false, targetDir)
        }
    }
}
