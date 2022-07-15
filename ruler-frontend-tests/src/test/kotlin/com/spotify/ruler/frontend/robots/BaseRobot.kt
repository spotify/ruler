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
import com.spotify.ruler.frontend.testutil.text
import org.openqa.selenium.By.id
import org.openqa.selenium.By.linkText
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfAllElements
import org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait

fun start(driver: WebDriver) = BreakdownRobot(driver)

/** Base class for global actions, all other testing robots should extend from this one. */
abstract class BaseRobot<T : BaseRobot<T>>(protected val driver: WebDriver) {
    abstract fun self(): T

    /** Selects the breakdown tab in the tab menu. */
    fun navigateToBreakdownTab(): BreakdownRobot {
        driver.findElement(linkText("Breakdown")).click()
        return BreakdownRobot(driver)
    }

    /** Selects the insights tab in the tab menu. */
    fun navigateToInsightsTab(): InsightsRobot {
        driver.findElement(linkText("Insights")).click()
        return InsightsRobot(driver)
    }

    /** Selects the ownership tab in the tab menu. */
    fun navigateToOwnershipTab(): OwnershipRobot {
        driver.findElement(linkText("Ownership")).click()
        return OwnershipRobot(driver)
    }

    /** Selected the dynamic features tab in the tab menu. */
    fun navigateToDynamicFeaturesTab(): DynamicFeaturesRobot {
        driver.findElement(linkText("Dynamic features")).click()
        return DynamicFeaturesRobot(driver)
    }

    /** Clicks on the element with the given [text]. */
    fun click(text: String): T {
        driver.findElement(text(text)).click()
        return self()
    }

    /** Selects download size in the global size type selection dropdown. */
    fun selectDownloadSize(): T {
        val dropdown = Select(driver.findElement(id("size-type-dropdown")))
        dropdown.selectByVisibleText("Download size")
        return self()
    }

    /** Selects install size in the global size type selection dropdown. */
    fun selectInstallSize(): T {
        val dropdown = Select(driver.findElement(id("size-type-dropdown")))
        dropdown.selectByVisibleText("Install size")
        return self()
    }

    /** Asserts that an element with the given [text] is visible (or becomes visible within a certain duration). */
    fun assertVisible(text: String): T {
        WebDriverWait(driver, WAIT_DURATION).until(visibilityOfElementLocated(text(text)))
        return self()
    }

    /** Asserts that no element with the given [text] is visible (after a certain maximum duration). */
    fun assertNotVisible(text: String): T {
        val elements = driver.findElements(text(text))
        WebDriverWait(driver, WAIT_DURATION).until(invisibilityOfAllElements(elements))
        return self()
    }
}
