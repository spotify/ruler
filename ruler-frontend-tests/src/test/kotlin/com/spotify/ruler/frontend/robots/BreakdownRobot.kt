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

package com.spotify.ruler.frontend.robots

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.frontend.testutil.sibling
import com.spotify.ruler.frontend.testutil.text
import org.openqa.selenium.WebDriver

/** Testing robot specifically for the breakdown page. */
class BreakdownRobot(driver: WebDriver) : BaseRobot<BreakdownRobot>(driver) {
    override fun self() = this

    /** Clicks and toggles (expands or hides) a given [component]. */
    fun toggleComponent(component: String) = apply {
        val element = driver.findElement(text(component))
        element.click()
    }

    /** Assets that the size displayed for a given [component] matches the expected [size]. */
    fun assertComponentSize(component: String, size: String) = apply {
        val element = driver.findElement(sibling(component))
        assertThat(element.text).isEqualTo(size)
    }
}
