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

package com.spotify.ruler.plugin.ownership

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.models.ComponentType
import org.junit.jupiter.api.Test

class OwnershipInfoTest {
    private val entries = listOf(
        OwnershipEntry(":foo:bar", "internal-component-owner"),
        OwnershipEntry("com.spotify:main", "external-component-owner"),
    )
    private val ownershipInfo = OwnershipInfo(entries, "default-owner")

    @Test
    fun `Internal component owner is found`() {
        val owner = ownershipInfo.getOwner(":foo:bar", ComponentType.INTERNAL)
        assertThat(owner).isEqualTo("internal-component-owner")
    }

    @Test
    fun `Internal component owner is not found there is no entry`() {
        val owner = ownershipInfo.getOwner(":foo:bar:baz", ComponentType.INTERNAL)
        assertThat(owner).isEqualTo("default-owner")
    }

    @Test
    fun `Internal component owner is not found when queried for internal components`() {
        val owner = ownershipInfo.getOwner(":foo:bar", ComponentType.EXTERNAL)
        assertThat(owner).isEqualTo("default-owner")
    }

    @Test
    fun `External component owner is found`() {
        val owner = ownershipInfo.getOwner("com.spotify:main:1.0.0", ComponentType.EXTERNAL)
        assertThat(owner).isEqualTo("external-component-owner")
    }

    @Test
    fun `External component owner is not found there is no entry`() {
        val owner = ownershipInfo.getOwner("com.spotify:other:1.0.0", ComponentType.EXTERNAL)
        assertThat(owner).isEqualTo("default-owner")
    }

    @Test
    fun `External component owner is not found when queried for internal components`() {
        val owner = ownershipInfo.getOwner("com.spotify:main:1.0.0", ComponentType.INTERNAL)
        assertThat(owner).isEqualTo("default-owner")
    }
}
