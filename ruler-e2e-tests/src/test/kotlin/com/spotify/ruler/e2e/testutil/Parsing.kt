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

package com.spotify.ruler.e2e.testutil

import com.spotify.ruler.models.AppReport
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Paths
import kotlin.io.path.readText

/** Parses and returns the sample project report of the given [app] and [variant]. */
fun parseReport(app: String, variant: String): AppReport {
    val path = Paths.get("..", "sample", app, "build", "reports", "ruler", variant, "report.json")
    return Json.decodeFromString(path.readText())
}
