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

package com.spotify.ruler.common.apk

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ApkParserTest {
    private val parser = ApkParser()
    private val apkFile = Paths.get("src", "test", "resources", "test.apk").toFile()

    @Test
    fun `APK entries are parsed`() {
        val entries = parser.parse(apkFile)

        assertThat(entries).contains(ApkEntry.Default("/AndroidManifest.xml", 776, 776))
        assertThat(entries).contains(ApkEntry.Default("/res/layout/activity_main.xml", 257, 257))
        assertThat(entries).contains(ApkEntry.Default("/resources.arsc", 212, 768))
    }

    @Test
    fun `Contents of DEX files are parsed`() {
        val entries = parser.parse(apkFile)
        val dex = entries.filterIsInstance<ApkEntry.Dex>().single()

        assertThat(dex.classes).contains(ApkEntry.Default("Lcom/spotify/ruler/sample/MainActivity;", 154, 154))
    }
}
