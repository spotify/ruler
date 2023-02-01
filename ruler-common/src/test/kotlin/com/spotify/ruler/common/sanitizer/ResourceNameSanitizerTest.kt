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

class ResourceNameSanitizerTest {
    private val sanitizer = ResourceNameSanitizer("res/anim/foo.xml -> [res/raw/b.xml]")

    @Test
    fun `Unknown resource files names are not changed`() {
        val sanitized = sanitizer.sanitize("/res/anim/bar.xml")
        assertThat(sanitized).isEqualTo("/res/anim/bar.xml")
    }

    @Test
    fun `Obfuscated resource file names are de-obfuscated`() {
        val sanitized = sanitizer.sanitize("/res/raw/b.xml")
        assertThat(sanitized).isEqualTo("/res/anim/foo.xml")
    }
}
