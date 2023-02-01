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

package com.spotify.ruler.common.report

import java.io.File

/** Responsible for generating HTML reports. */
class HtmlReporter {

    /**
     * Generates a HTML report.
     *
     * @param json JSON report data on which the report will be based.
     * @param targetDir Directory where the generated report will be located
     * @return Generated HTML report file
     */
    fun generateReport(json: String, targetDir: File): File {
        var html = readResourceFile("index.html")

        // Inline Javascript
        val javascript = readResourceFile("ruler-frontend.js")
        html = html.replaceFirst("<script src=\"ruler-frontend.js\"></script>", "<script>$javascript</script>")

        // Inline JSON data
        html = html.replaceFirst("\"REPLACE_ME\"", "`$json`")

        val reportFile = targetDir.resolve("report.html")
        reportFile.writeText(html)
        return reportFile
    }

    private fun readResourceFile(fileName: String): String {
        val url = requireNotNull(javaClass.getResource("/$fileName"))
        return url.readText()
    }
}
