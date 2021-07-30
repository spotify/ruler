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

import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.FileType
import kotlinext.js.require
import kotlinx.browser.document
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import react.dom.render

fun main() {
    require("bootstrap/dist/css/bootstrap.css")
    require("bootstrap/dist/js/bootstrap.bundle.js")

    val report = getReportData()
    render(document.getElementById("root")) {
        reportCard(report)
    }
}

fun getReportData(): AppReport = try {
    getRealReportData() // Try to read real data
} catch (ignored: SerializationException) {
    getFakeReportData() // If not real data is provided, fall back to fake data
}

// Provides the real report data, for the generated report
fun getRealReportData(): AppReport {
    val json = "REPLACE_ME" // Will be replaced with the report data in JSON format
    return Json.decodeFromString(json)
}

// Provides fake report data, to be able to work on the frontend independently
@Suppress("MagicNumber")
fun getFakeReportData(): AppReport = AppReport("com.spotify.music", "1.2.3", "release", 750, 1050, listOf(
    AppComponent(":lib", 500, 600, listOf(
        AppFile("/assets/license.html", FileType.ASSET, 500, 600),
    )),
    AppComponent(":app", 250, 450, listOf(
        AppFile("/res/layout/activity_main.xml", FileType.RESOURCE, 150, 250),
        AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200),
    )),
))
