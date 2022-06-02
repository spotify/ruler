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

import com.spotify.ruler.frontend.testutil.WAIT_DURATION
import com.spotify.ruler.frontend.testutil.ancestor
import com.spotify.ruler.frontend.testutil.text
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfNestedElementsLocatedBy
import org.openqa.selenium.support.ui.WebDriverWait

/** Testing robot specifically for the breakdown page. */
class BreakdownRobot(driver: WebDriver) : BaseRobot<BreakdownRobot>(driver) {
    override fun self() = this

    /** Assets that the size displayed for a given [component] matches the expected [size]. */
    fun assertComponentSize(component: String, size: String) = apply {
        val ancestor = driver.findElement(ancestor(component))
        WebDriverWait(driver, WAIT_DURATION).until(visibilityOfNestedElementsLocatedBy(ancestor, text(size)))
    }
}
