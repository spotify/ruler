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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.spotify.ruler.frontend.binding.ApexCharts
import com.spotify.ruler.frontend.chart.BarChartConfig
import com.spotify.ruler.frontend.chart.ChartConfig
import com.spotify.ruler.frontend.chart.seriesOf
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.Measurable
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text


@Composable
fun Insights(    components: List<AppComponent>,
                 hasFileLevelInfo: Boolean
) {
    Div(attrs  = {
        classes("row", "mb-3")
    }) {
        ComponentTypeGraphs(components)
    }

    if (hasFileLevelInfo) {
        val componentFiles = components.mapNotNull(AppComponent::files).flatten()
        Div(attrs = {
            classes("row", "mb-3")
        }) {
            FileTypeGraphs(
                componentFiles
            )
        }
        Div(attrs = {
            classes("row")
        }) {
            ResourceTypeGraphs(
                componentFiles
            )
        }
    }
}

@Composable
fun FileTypeGraphs(files: List<AppFile>) {
    val labels = arrayOf("Classes", "Resources", "Assets", "Native libraries", "Other")
    val downloadSizes = LongArray(labels.size)
    val installSizes = LongArray(labels.size)
    val fileCounts = LongArray(labels.size)

    files.forEach { file ->
        val index = file.type.ordinal
        downloadSizes[index] += file.getSize(Measurable.SizeType.DOWNLOAD)
        installSizes[index] += file.getSize(Measurable.SizeType.INSTALL)
        fileCounts[index]++
    }

    Chart(
        id = "file-type-distribution-size-chart",
        title = "File type distribution (size)",
        description = "Shows the accumulated app size for each file type.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Download size", downloadSizes), seriesOf("Install size", installSizes)),
            chartHeight = 350,
            yAxisFormatter = ::formatSize,
        )
    )

    Chart(
        id = "file-type-distribution-count-chart",
        title = "File type distribution (file count)",
        description = "Shows how many files of a certain type are contained in the app.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Files", fileCounts)),
            chartHeight = 350,
        )
    )
}


@Composable
fun ComponentTypeGraphs(components: List<AppComponent>) {
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

    Chart(
        id = "component-type-distribution-size-chart",
        title = "Component type distribution (size)",
        description = "Shows the accumulated app size for each component type.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Download size", downloadSizes), seriesOf("Install size", installSizes)),
            chartHeight = 250,
            horizontal = true,
            xAxisFormatter = ::formatSize,
        )
    )
    Chart(
        id = "component-type-distribution-count-chart",
        title = "Component type distribution (component count)",
        description = "Shows how many components of a certain type are contained in the app.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Components", fileCounts)),
            chartHeight = 250,
            horizontal = true,
        )
    )
}


@Composable
fun ResourceTypeGraphs(files: List<AppFile>) {
    val labels = arrayOf("Drawable", "Layout", "Raw", "Values", "Font", "Other")
    val downloadSizes = LongArray(labels.size)
    val installSizes = LongArray(labels.size)
    val fileCounts = LongArray(labels.size)

    files.filter { it.resourceType != null }.forEach { file ->
        val index = file.resourceType!!.ordinal
        downloadSizes[index] += file.getSize(Measurable.SizeType.DOWNLOAD)
        installSizes[index] += file.getSize(Measurable.SizeType.INSTALL)
        fileCounts[index]++
    }

    Chart(
        id = "resource-type-distribution-size-chart",
        title = "Resource type distribution (size)",
        description = "Shows the accumulated app size for each resource type.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Download size", downloadSizes), seriesOf("Install size", installSizes)),
            chartHeight = 350,
            yAxisFormatter = ::formatSize,
        )
    )
    Chart(
        id = "resource-type-distribution-count-chart",
        title = "Resource type distribution (file count)",
        description = "Shows how many files of a certain resource type are contained in the app.",
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Files", fileCounts)),
            chartHeight = 350,
        )
    )
}

@Composable
fun Chart(
    id: String,
    title: String,
    description: String,
    config: ChartConfig
) {
    Div(attrs = {
        classes("col")
    }){
        H4 { Text(title) }
        P(attrs  = {
            classes("text-muted")
        }) {
            Text(description)
        }
        Div(attrs = {
            id(id)
        }) {
            DisposableEffect(config) {
                val chart = ApexCharts(
                    document.getElementById(id),
                    config.getOptions()
                )
                chart.render()

                onDispose {
                    chart.destroy()
                }
            }
        }
    }
}
