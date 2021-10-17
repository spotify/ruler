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

package com.spotify.ruler.frontend.components

import com.bnorm.react.RFunction
import com.spotify.ruler.frontend.binding.ApexCharts
import com.spotify.ruler.frontend.binding.Uuid
import com.spotify.ruler.frontend.chart.ChartConfig
import com.spotify.ruler.frontend.chart.BarChartConfig
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.frontend.chart.seriesOf
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.Measurable
import kotlinx.browser.document
import kotlinx.html.id
import react.RBuilder
import react.dom.div
import react.dom.h4
import react.dom.p
import react.useEffect

@RFunction
fun RBuilder.insights(components: List<AppComponent>) {
    div(classes = "row mb-3") {
        fileTypeGraphs(components)
    }
    div(classes = "row") {
        componentTypeGraphs(components)
    }
}

@RFunction
fun RBuilder.fileTypeGraphs(components: List<AppComponent>) {
    val labels = arrayOf("Classes", "Resources", "Assets", "Native libraries", "Other")
    val downloadSizes = LongArray(labels.size)
    val installSizes = LongArray(labels.size)
    val fileCounts = LongArray(labels.size)

    components.flatMap(AppComponent::files).forEach { file ->
        val index = file.type.ordinal
        downloadSizes[index] += file.getSize(Measurable.SizeType.DOWNLOAD)
        installSizes[index] += file.getSize(Measurable.SizeType.INSTALL)
        fileCounts[index]++
    }

    chart(
        title = "File type distribution (size)",
        description = "Shows the accumulated app size for each file type.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Download size", downloadSizes), seriesOf("Install size", installSizes)),
            chartHeight = 350,
            yAxisFormatter = ::formatSize,
        ),
    )
    chart(
        title = "File type distribution (file count)",
        description = "Shows how many files of a certain type are contained in the app.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Files", fileCounts)),
            chartHeight = 350,
        ),
    )
}

@RFunction
fun RBuilder.componentTypeGraphs(components: List<AppComponent>) {
    val labels = arrayOf("Internal", "External")
    val downloadSizes = LongArray(labels.size)
    val installSizes = LongArray(labels.size)
    val fileCounts = LongArray(labels.size)

    components.forEach { component ->
        val index = component.type.ordinal
        downloadSizes[index] += component.getSize(Measurable.SizeType.DOWNLOAD)
        installSizes[index] += component.getSize(Measurable.SizeType.INSTALL)
        fileCounts[index]++
    }

    chart(
        title = "Component type distribution (size)",
        description = "Shows the accumulated app size for each component type.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Download size", downloadSizes), seriesOf("Install size", installSizes)),
            chartHeight = 250,
            horizontal = true,
            xAxisFormatter = ::formatSize,
        ),
    )
    chart(
        title = "Component type distribution (component count)",
        description = "Shows how many components of a certain type are contained in the app.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Components", fileCounts)),
            chartHeight = 250,
            horizontal = true,
        ),
    )
}

@RFunction
fun RBuilder.chart(title: String, description: String, config: ChartConfig) {
    div(classes = "col") {
        h4 { +title }
        p(classes = "text-muted") { +description }
        div {
            attrs.id = Uuid.v4()
            useEffect {
                val chart = ApexCharts(document.getElementById(attrs.id), config.getOptions())
                chart.render()
                cleanup {
                    chart.destroy()
                }
            }
        }
    }
}
