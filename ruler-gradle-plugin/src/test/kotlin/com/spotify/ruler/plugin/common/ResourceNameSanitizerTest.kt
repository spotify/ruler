package com.spotify.ruler.plugin.common

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ResourceNameSanitizerTest {
    private val sanitizer = ResourceNameSanitizer("res/anim/foo.xml -> [res/raw/b.xml]")

    @Test
    fun `Obfuscated resource file names are de-obfuscated`() {
        val sanitized = sanitizer.sanitize("/res/raw/b.xml")
        assertThat(sanitized).isEqualTo("/res/anim/foo.xml")
    }
}
