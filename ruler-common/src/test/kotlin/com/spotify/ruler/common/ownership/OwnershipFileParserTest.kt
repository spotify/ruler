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

package com.spotify.ruler.common.ownership

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OwnershipFileParserTest {
    private val parser = OwnershipFileParser()

    @Test
    fun `File with single entry can be parsed`(@TempDir targetDir: File) {
        val ownershipFile = targetDir.resolve("ownership.yaml")
        ownershipFile.writeText("""
            - identifier: :foo:bar
              owner: team-a
        """.trimIndent())

        val entries = parser.parse(ownershipFile)
        assertThat(entries).containsExactly(OwnershipEntry(":foo:bar", "team-a"))
    }

    @Test
    fun `File with multiple entries can be parsed`(@TempDir targetDir: File) {
        val ownershipFile = targetDir.resolve("ownership.yaml")
        ownershipFile.writeText("""
            - identifier: :foo:bar
              owner: team-a
              
            - identifier: :foo:baz
              owner: team-b
        """.trimIndent())

        val entries = parser.parse(ownershipFile)
        assertThat(entries).containsExactly(
            OwnershipEntry(":foo:bar", "team-a"),
            OwnershipEntry(":foo:baz", "team-b"),
        )
    }

    @Test
    fun `Entries with extra data can be parsed`(@TempDir targetDir: File) {
        val ownershipFile = targetDir.resolve("ownership.yaml")
        ownershipFile.writeText("""
            - identifier: :foo:bar
              owner: team-a
              extra: xyz
        """.trimIndent())

        val entries = parser.parse(ownershipFile)
        assertThat(entries).containsExactly(OwnershipEntry(":foo:bar", "team-a"))
    }

    @Test
    fun `Entries without all required fields lead to an exception`(@TempDir targetDir: File) {
        val ownershipFile = targetDir.resolve("ownership.yaml")
        ownershipFile.writeText("""
            - identifier: :foo:bar
        """.trimIndent())

        assertThrows<IllegalStateException> { parser.parse(ownershipFile) }
    }

    @Test
    fun `Invalid file format leads to an exception`(@TempDir targetDir: File) {
        val ownershipFile = targetDir.resolve("ownership.yaml")
        ownershipFile.writeText("""
            this is not valid YAML
        """.trimIndent())

        assertThrows<IllegalStateException> { parser.parse(ownershipFile) }
    }
}
