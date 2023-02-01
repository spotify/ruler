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

package com.spotify.ruler.common.sanitizer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ClassNameSanitizerTest {
    private val sanitizer = ClassNameSanitizer("foo.bar.BazActivity -> a.b.C:")

    @Test
    fun `Java type notation pre- and suffix are removed`() {
        val sanitized = sanitizer.sanitize("Lfoo.bar.BazActivity;")
        assertThat(sanitized).isEqualTo("foo.bar.BazActivity")
    }

    @Test
    fun `Class file ending is removed`() {
        val sanitized = sanitizer.sanitize("foo.bar.BazActivity.class")
        assertThat(sanitized).isEqualTo("foo.bar.BazActivity")
    }

    @Test
    fun `Directory notation is normalized`() {
        val sanitized = sanitizer.sanitize("foo/bar/BazActivity")
        assertThat(sanitized).isEqualTo("foo.bar.BazActivity")
    }

    @Test
    fun `Obfuscated classes are de-obfuscated`() {
        val sanitized = sanitizer.sanitize("a.b.C")
        assertThat(sanitized).isEqualTo("foo.bar.BazActivity")
    }
}
