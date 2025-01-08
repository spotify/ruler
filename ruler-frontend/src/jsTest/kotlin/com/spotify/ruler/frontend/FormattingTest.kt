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

package com.spotify.ruler.frontend

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattingTest {

    @Test
    fun formatSize() {
        assertEquals("104.0 B", formatSize(104.0))
        assertEquals("4.3 KB", formatSize(4.3 * 1024))
        assertEquals("26.0 MB", formatSize(26.0 * 1024 * 1024))
    }

    @Test
    fun formatPercentage() {
        assertEquals("100.00 %", formatPercentage(100, 100))
        assertEquals("6.73 %", formatPercentage(55, 817))
        assertEquals("37.50 %", formatPercentage(75, 200))
        assertEquals("166.67 %", formatPercentage(150, 90))
    }
}
