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
class OwnershipTest {

    @Test
    fun `Browsing between ownership overview pages changes the content`(driver: WebDriver) {
        start(driver)
            .navigateToOwnershipTab()
            .assertVisible("lib-team")
            .selectPage(2)
            .assertVisible("performance-team")
            .selectPage(1)
            .assertVisible("lib-team")
    }

    @Test
    fun `Switching the selected owner changes the displayed values`(driver: WebDriver) {
        start(driver)
            .navigateToOwnershipTab()
            .assertVisible(":app")
            .assertVisible("250.0 B")
            .assertVisible("450.0 B")
            .assertNotVisible(":lib")
            .selectOwner("lib-team")
            .assertVisible(":lib")
            .assertVisible("500.0 B")
            .assertVisible("600.0 B")
            .assertNotVisible(":app")
    }
}
