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

package com.spotify.ruler.e2e

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.e2e.testutil.Correspondence
import com.spotify.ruler.e2e.testutil.FileMatcher
import com.spotify.ruler.e2e.testutil.parseReport
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.ResourceType
import org.junit.jupiter.api.Test

class ReleaseReportTest {

    // Use the report generated by the sample app for verification
    private val report = parseReport("app", "release")

    @Test
    fun `Metadata is reported correctly`() {
        assertThat(report.name).isEqualTo("com.spotify.ruler.sample")
        assertThat(report.version).isEqualTo("1.0")
        assertThat(report.variant).isEqualTo("release")
    }

    @Test
    fun `App component is reported correctly`() {
        val app = report.components.single { component -> component.name == ":sample:app" }
        assertThat(app.type).isEqualTo(ComponentType.INTERNAL)
        assertThat(app.owner).isEqualTo("default-team")
        // Filters out META-INF stuff coming from androdiX libraries as they are too many.
        val files = app.files?.filter { !it.name.startsWith("/META-INF/androidx") }

        assertThat(files).comparingElementsUsing(Correspondence.file()).containsExactly(
            FileMatcher("com.spotify.ruler.sample.app.MainActivity", FileType.CLASS, "main-team"),
            FileMatcher("/res/layout/activity_main.xml", FileType.RESOURCE, "main-team", ResourceType.LAYOUT),
            FileMatcher("/res/drawable/test_drawable.xml", FileType.RESOURCE, "default-team", ResourceType.DRAWABLE),
            FileMatcher("/AndroidManifest.xml", FileType.OTHER, "default-team"),
            FileMatcher("/resources.arsc", FileType.OTHER, "default-team"),
            FileMatcher("/META-INF/com/android/build/gradle/app-metadata.properties", FileType.OTHER, "default-team"),
        )
    }

    @Test
    fun `Lib component is reported correctly`() {
        val lib = report.components.single { component -> component.name == ":sample:lib" }
        assertThat(lib.type).isEqualTo(ComponentType.INTERNAL)
        assertThat(lib.owner).isEqualTo("lib-team")

        assertThat(lib.files).comparingElementsUsing(Correspondence.file()).containsExactly(
            FileMatcher("com.spotify.ruler.sample.lib.LibActivity", FileType.CLASS, "lib-team"),
            FileMatcher("com.spotify.ruler.sample.lib.ClassToObfuscate", FileType.CLASS, "lib-team"),
            FileMatcher("/res/layout/activity_lib.xml", FileType.RESOURCE, "lib-team", ResourceType.LAYOUT),
            FileMatcher("/res/layout-v22/activity_lib.xml", FileType.RESOURCE, "lib-team", ResourceType.LAYOUT),
            FileMatcher("/assets/asset.txt", FileType.ASSET, "lib-team"),
        )
    }

    @Test
    fun `Dynamic component is reported correctly`() {
        val dynamic = report.dynamicFeatures.single { feature -> feature.name == "dynamic" }
        assertThat(dynamic.owner).isEqualTo("dynamic-team")

        assertThat(dynamic.files).comparingElementsUsing(Correspondence.file()).containsExactly(
            FileMatcher("/AndroidManifest.xml", FileType.OTHER, "dynamic-team"),
            FileMatcher("/resources.arsc", FileType.OTHER, "dynamic-team"),
            FileMatcher("com.spotify.ruler.sample.dynamic.DynamicActivity", FileType.CLASS, "dynamic-team"),
            FileMatcher("/res/layout/activity_dynamic.xml", FileType.RESOURCE, "dynamic-team", ResourceType.LAYOUT),
        )
    }

    @Test
    fun `Component sizes add up correctly`() {
        var downloadSize = 0L
        var installSize = 0L
        report.components.forEach { component ->
            downloadSize += component.downloadSize
            installSize += component.installSize
        }
        assertThat(downloadSize).isEqualTo(report.downloadSize)
        assertThat(installSize).isEqualTo(report.installSize)
    }

    @Test
    fun `File sizes add up correctly`() {
        report.components.forEach { component ->
            var downloadSize = 0L
            var installSize = 0L
            component.files?.forEach { file ->
                downloadSize += file.downloadSize
                installSize += file.installSize
            }
            assertThat(downloadSize).isEqualTo(component.downloadSize)
            assertThat(installSize).isEqualTo(component.installSize)
        }
    }

    @Test
    fun `Dynamic feature sizes add up correctly`() {
        report.dynamicFeatures.forEach { feature ->
            var downloadSize = 0L
            var installSize = 0L
            feature.files?.forEach { file ->
                downloadSize += file.downloadSize
                installSize += file.installSize
            }
            assertThat(downloadSize).isEqualTo(feature.downloadSize)
            assertThat(installSize).isEqualTo(feature.installSize)
        }
    }
}
