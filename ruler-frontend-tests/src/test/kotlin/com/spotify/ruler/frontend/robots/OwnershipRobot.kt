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

import com.spotify.ruler.frontend.testutil.text
import org.openqa.selenium.By.className
import org.openqa.selenium.By.id
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.Select

/** Testing robot specifically for the ownership page. */
class OwnershipRobot(driver: WebDriver) : BaseRobot<OwnershipRobot>(driver) {
    override fun self() = this

    /** Selects the page with the given [number] in the ownership overview chart. */
    fun selectPage(number: Int) = apply {
        val pagination = driver.findElement(className("pagination"))
        val page = pagination.findElement(text(number.toString()))
        page.click()
    }

    /** Selects the given [owner] in the owner selection dropdown. */
    fun selectOwner(owner: String) = apply {
        val dropdown = Select(driver.findElement(id("owner-dropdown")))
        dropdown.selectByVisibleText(owner)
    }
}
