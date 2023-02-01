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

package com.spotify.ruler.common.dependency

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.common.sanitizer.ClassNameSanitizer
import com.spotify.ruler.models.ComponentType
import org.junit.jupiter.api.Test

class DependencySanitizerTest {
    private val sanitizer = DependencySanitizer(ClassNameSanitizer(null))

    @Test
    fun `Prefix is removed from Gradle module identifiers`() {
        val dirty = DependencyEntry.Default("licenses.html", "project :lib")
        val clean = sanitizer.sanitize(listOf(dirty))

        assertThat(clean).containsEntry("licenses.html", listOf(DependencyComponent(":lib", ComponentType.INTERNAL)))
    }

    @Test
    fun `Class names are sanitized`() {
        val dirty = DependencyEntry.Class("com/spotify/MainActivity.class", "com.spotify:main:1.0.0")
        val clean = sanitizer.sanitize(listOf(dirty))

        assertThat(clean).containsEntry("com.spotify.MainActivity", listOf(
            DependencyComponent("com.spotify:main:1.0.0", ComponentType.EXTERNAL),
        ))
    }

    @Test
    fun `Component types are recognized`() {
        val dirty = listOf(
            DependencyEntry.Default("foo.txt", ":foo"),
            DependencyEntry.Default("bar.txt", "org.bar:bar:1.0.0"),
        )
        val clean = sanitizer.sanitize(dirty)

        assertThat(clean).containsEntry("foo.txt", listOf(DependencyComponent(":foo", ComponentType.INTERNAL)))
        assertThat(clean).containsEntry("bar.txt", listOf(
            DependencyComponent("org.bar:bar:1.0.0", ComponentType.EXTERNAL),
        ))
    }

    @Test
    fun `Files are mapped to their components`() {
        val dirty = listOf(
            DependencyEntry.Default("foo.txt", ":foo"),
            DependencyEntry.Default("bar.txt", ":bar"),
            DependencyEntry.Default("baz.txt", ":baz"),
        )
        val clean = sanitizer.sanitize(dirty)

        assertThat(clean).containsEntry("foo.txt", listOf(DependencyComponent(":foo", ComponentType.INTERNAL)))
        assertThat(clean).containsEntry("bar.txt", listOf(DependencyComponent(":bar", ComponentType.INTERNAL)))
        assertThat(clean).containsEntry("baz.txt", listOf(DependencyComponent(":baz", ComponentType.INTERNAL)))
    }

    @Test
    fun `Files can appear in multiple components`() {
        val dirty = listOf(
            DependencyEntry.Default("test.txt", ":foo"),
            DependencyEntry.Default("test.txt", ":bar"),
            DependencyEntry.Default("test.txt", ":baz"),
        )
        val clean = sanitizer.sanitize(dirty)

        assertThat(clean).containsEntry("test.txt", listOf(
            DependencyComponent(":foo", ComponentType.INTERNAL),
            DependencyComponent(":bar", ComponentType.INTERNAL),
            DependencyComponent(":baz", ComponentType.INTERNAL),
        ))
    }

    @Test
    fun `Windows-style paths are converted to UNIX-style paths`() {
        val dirty = DependencyEntry.Default("\\windows\\path\\test.txt", ":test")
        val clean = sanitizer.sanitize(listOf(dirty))

        assertThat(clean).containsEntry("/windows/path/test.txt", listOf(
            DependencyComponent(":test", ComponentType.INTERNAL),
        ))
    }
}
