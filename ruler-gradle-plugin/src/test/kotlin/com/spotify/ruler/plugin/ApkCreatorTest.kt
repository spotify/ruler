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
import com.spotify.ruler.common.models.DeviceSpec
import com.spotify.ruler.plugin.apk.ApkCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Paths

class ApkCreatorTest {
    private val creator = ApkCreator(File(""))
    private val bundleFile = Paths.get("src", "test", "resources", "test.aab").toFile()
    private val deviceSpec = DeviceSpec("arm64-v8a", "en", 480, 27)

    @Test
    fun `Split APKs are created`(@TempDir targetDir: File) {
        val splits = creator.createSplitApks(bundleFile, deviceSpec, targetDir)

        assertThat(splits).hasSize(1)
        assertThat(splits).containsKey("base")
        splits.values.flatten().forEach { apk ->
            assertThat(apk.exists()).isTrue()
        }
    }

    @Test
    fun `Existing APKs are overwritten`(@TempDir targetDir: File) {
        repeat(2) {
            creator.createSplitApks(bundleFile, deviceSpec, targetDir)
        }
    }
}
