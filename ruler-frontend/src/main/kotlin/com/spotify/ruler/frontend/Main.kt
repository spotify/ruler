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

import com.spotify.ruler.frontend.components.Report
import com.spotify.ruler.models.AppReport
import kotlinx.browser.document
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.renderComposable


fun main() {
    kotlinext.js.require("./style.css")
    kotlinext.js.require("bootstrap/dist/css/bootstrap.css")
    kotlinext.js.require("bootstrap/dist/js/bootstrap.bundle.js")

    // Load and show the favicon
    val favicon = kotlinext.js.require("./favicon.svg").toString()
    val link = document.createElement("link").apply {
        setAttribute("rel", "icon")
        setAttribute("href", favicon)
    }
    document.head?.append(link)

    // Load and deserialize the report data
    val rawReport = kotlinext.js.require("report.json").toString()
    val report = Json.decodeFromString<AppReport>(rawReport)

    // Visualize and display the report data
    renderComposable(rootElementId = "root") {
        Report(report)
    }
}
