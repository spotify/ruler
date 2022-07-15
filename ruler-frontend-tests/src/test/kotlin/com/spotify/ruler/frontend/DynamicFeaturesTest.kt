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

import com.spotify.ruler.frontend.robots.start
import com.spotify.ruler.frontend.testutil.WebDriverExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.WebDriver

@ExtendWith(WebDriverExtension::class)
class DynamicFeaturesTest {

    @Test
    fun `Switching between download and install size changes the displayed size`(driver: WebDriver) {
        start(driver)
            .navigateToDynamicFeaturesTab()
            .assertFeatureSize("dynamic", "500.0 B")
            .selectInstallSize()
            .assertFeatureSize("dynamic", "800.0 B")
            .selectDownloadSize()
            .assertFeatureSize("dynamic", "500.0 B")
    }

    @Test
    fun `Toggling list items reveals and hides details`(driver: WebDriver) {
        start(driver)
            .navigateToDynamicFeaturesTab()
            .assertNotVisible("com.spotify.DynamicActivity")
            .click("dynamic")
            .assertVisible("com.spotify.DynamicActivity")
            .click("dynamic")
            .assertNotVisible("com.spotify.DynamicActivity")
    }
}
