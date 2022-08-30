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
class CommonTest {

    @Test
    fun `Overall report details and sizes are displayed`(driver: WebDriver) {
        start(driver)
            .assertVisible("com.spotify.music")
            .assertVisible("Version 1.2.3 (release)")
            .assertVisible("2.8 KB")
            .assertVisible("4.7 KB")
    }

    @Test
    fun `Navigation between tabs changes the displayed content`(driver: WebDriver) {
        start(driver)
            .assertVisible("Breakdown (13 components)")
            .navigateToInsightsTab()
            .assertVisible("File type distribution (size)")
            .assertVisible("File type distribution (file count)")
            .assertVisible("Component type distribution (size)")
            .assertVisible("Component type distribution (component count)")
            .assertVisible("Resource type distribution (size)")
            .assertVisible("Resource type distribution (file count)")
            .navigateToOwnershipTab()
            .assertVisible("Ownership overview")
            .assertVisible("Components and files grouped by owner")
            .navigateToDynamicFeaturesTab()
            .assertVisible("Dynamic features")
    }
}
