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

package com.spotify.ruler.frontend.testutil

import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.nio.file.Paths

/**
 * Test extension managing the lifecycle of the test browser. Opens the browser and loads the test report before each
 * test. Also shuts down the browser after a test has finished.
 */
class WebDriverExtension : BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    lateinit var driver: WebDriver

    override fun beforeAll(context: ExtensionContext) {
        WebDriverManager.chromedriver().setup()
    }

    override fun beforeEach(context: ExtensionContext) {
        val options = ChromeOptions()
            .addArguments("--headless")
            .addArguments("--window-size=1920x1080")
            .addArguments("--start-maximized")
            .addArguments("--no-sandbox")
            .addArguments("--disable-dev-shm-usage")
            .addArguments("--remote-allow-origins=*")

        driver = ChromeDriver(options)

        // Open and use the generated development report page for testing
        val reportPath = Paths.get("..", "ruler-frontend", "build", "dist", "js", "developmentExecutable", "index.html")
        require(reportPath.toFile().exists())
        driver.get(reportPath.toUri().toString())
    }

    override fun afterEach(context: ExtensionContext) {
        driver.quit()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == WebDriver::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return driver
    }
}
