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
import kotlinext.js.require
import kotlinx.browser.document
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import react.Fragment
import react.create
import react.dom.client.createRoot

fun main() {
    require("./style.css")

    require("bootstrap/dist/css/bootstrap.css")
    require("bootstrap/dist/js/bootstrap.bundle.js")

    // Load and show the favicon
    val favicon = kotlinext.js.require("./favicon.svg").toString()
    val link = document.createElement("link").apply {
        setAttribute("rel", "icon")
        setAttribute("href", favicon)
    }
    document.head?.append(link)

    // Load and deserialize the report data
    val rawReport = require("report.json").toString()
    val reportData = Json.decodeFromString<AppReport>(rawReport)

    // Visualize and display the report data
    val container =
        web.dom.document.getElementById("root") ?: error("Couldn't find root container!")
    createRoot(container).render(Fragment.create {
        Report {
            report = reportData
        }
    })
}
