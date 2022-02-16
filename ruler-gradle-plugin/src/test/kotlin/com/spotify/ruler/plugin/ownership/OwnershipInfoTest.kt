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
        OwnershipEntry(":wildcard:foo:*", "internal-wildcard-foo-owner"),
        OwnershipEntry(":wildcard:*", "internal-wildcard-owner"),
        OwnershipEntry(":wildcard:foo:bar", "internal-wildcard-foo-bar-owner"),
        OwnershipEntry("com.spotify:main", "external-component-owner"),
        OwnershipEntry("com.wildcard.spotify:*", "external-wildcard-spotify-owner"),
        OwnershipEntry("com.wildcard.*", "external-wildcard-owner"),
        OwnershipEntry("com.wildcard.spotify:foo", "external-wildcard-spotify-foo-owner"),
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
    fun `Internal component owner is found for wildcard entries`() {
        val owner = ownershipInfo.getOwner(":wildcard:test", ComponentType.INTERNAL)
        assertThat(owner).isEqualTo("internal-wildcard-owner")
    }

    @Test
    fun `Internal component owner is found for more specific wildcard entries`() {
        val owner = ownershipInfo.getOwner(":wildcard:foo:test", ComponentType.INTERNAL)
        assertThat(owner).isEqualTo("internal-wildcard-foo-owner")
    }

    @Test
    fun `Internal component owner is found for explicit entry when wildcard is present`() {
        val owner = ownershipInfo.getOwner(":wildcard:foo:bar", ComponentType.INTERNAL)
        assertThat(owner).isEqualTo("internal-wildcard-foo-bar-owner")
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

    @Test
    fun `External component owner is found for wildcard entries`() {
        val owner = ownershipInfo.getOwner("com.wildcard.test:test:1.0.0", ComponentType.EXTERNAL)
        assertThat(owner).isEqualTo("external-wildcard-owner")
    }

    @Test
    fun `External component owner is found for more specific wildcard entries`() {
        val owner = ownershipInfo.getOwner("com.wildcard.spotify:test:1.0.0", ComponentType.EXTERNAL)
        assertThat(owner).isEqualTo("external-wildcard-spotify-owner")
    }

    @Test
    fun `External component owner is found for explicit entry when wildcard is present`() {
        val owner = ownershipInfo.getOwner("com.wildcard.spotify:foo:1.0.0", ComponentType.EXTERNAL)
        assertThat(owner).isEqualTo("external-wildcard-spotify-foo-owner")
    }
}
