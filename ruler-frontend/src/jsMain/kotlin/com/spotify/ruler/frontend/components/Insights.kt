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

import com.spotify.ruler.frontend.binding.ApexCharts
import com.spotify.ruler.frontend.chart.BarChartConfig
import com.spotify.ruler.frontend.chart.ChartConfig
import com.spotify.ruler.frontend.chart.seriesOf
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.Measurable
import kotlinx.browser.document
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.p
import react.useEffect
import web.cssom.ClassName

external interface InsightsProps: Props {
    var components: List<AppComponent>
    var hasFileLevelInfo: Boolean
}

val Insights = FC<InsightsProps> { props ->
    Row {
        className = ClassName("mb-3")
        ComponentTypeGraphs {
            components = props.components
        }
    }

    if (props.hasFileLevelInfo) {
        val componentFiles = props.components.mapNotNull(AppComponent::files).flatten()
        Row {
            className = ClassName("mb-3")
            FileTypeGraphs {
                files =  componentFiles
            }
        }
        Row {
            ResourcesTypeGraphs {
                files = componentFiles
            }
        }
    }

}

external interface FileTypeGraphsProps : Props {
    var files: List<AppFile>
}

val FileTypeGraphs = FC<FileTypeGraphsProps> { props ->
    val labels = arrayOf("Classes", "Resources", "Assets", "Native libraries", "Other")
    val downloadSizes = LongArray(labels.size)
    val installSizes = LongArray(labels.size)
    val fileCounts = LongArray(labels.size)

    props.files.forEach { file ->
        val index = file.type.ordinal
        downloadSizes[index] += file.getSize(Measurable.SizeType.DOWNLOAD)
        installSizes[index] += file.getSize(Measurable.SizeType.INSTALL)
        fileCounts[index]++
    }

    Chart {
        id = "file-type-distribution-size-chart"
        title = "File type distribution (size)"
        description = "Shows the accumulated app size for each file type."
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(
                seriesOf("Download size", downloadSizes),
                seriesOf("Install size", installSizes)
            ),
            chartHeight = 350,
            yAxisFormatter = ::formatSize,
        )
    }
    Chart {
        id = "file-type-distribution-count-chart"
        title = "File type distribution (file count)"
        description = "Shows how many files of a certain type are contained in the app."
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Files", fileCounts)),
            chartHeight = 350,
        )
    }
}

external interface ComponentTypeGraphsProps : Props {
    var components: List<AppComponent>
}

val ComponentTypeGraphs = FC<ComponentTypeGraphsProps> { props ->
    val labels = arrayOf("Internal", "External")
    val downloadSizes = LongArray(labels.size)
    val installSizes = LongArray(labels.size)
    val fileCounts = LongArray(labels.size)

    props.components.forEach { component ->
        val index = component.type.ordinal
        downloadSizes[index] += component.getSize(Measurable.SizeType.DOWNLOAD)
        installSizes[index] += component.getSize(Measurable.SizeType.INSTALL)
        fileCounts[index]++
    }

    Chart {
        id = "component-type-distribution-size-chart"
        title = "Component type distribution (size)"
        description = "Shows the accumulated app size for each component type."
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(
                seriesOf("Download size", downloadSizes),
                seriesOf("Install size", installSizes)
            ),
            chartHeight = 250,
            horizontal = true,
            xAxisFormatter = ::formatSize,
        )
    }
    Chart {
        id = "component-type-distribution-count-chart"
        title = "Component type distribution (component count)"
        description = "Shows how many components of a certain type are contained in the app."
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Components", fileCounts)),
            chartHeight = 250,
            horizontal = true,
        )
    }
}

external interface ResourcesTypeGraphsProps : Props {
    var files: List<AppFile>
}

val ResourcesTypeGraphs = FC<ResourcesTypeGraphsProps> { props ->
    val labels = arrayOf("Drawable", "Layout", "Raw", "Values", "Font", "Other")
    val downloadSizes = LongArray(labels.size)
    val installSizes = LongArray(labels.size)
    val fileCounts = LongArray(labels.size)

    props.files.filter { it.resourceType != null }.forEach { file ->
        val index = file.resourceType!!.ordinal
        downloadSizes[index] += file.getSize(Measurable.SizeType.DOWNLOAD)
        installSizes[index] += file.getSize(Measurable.SizeType.INSTALL)
        fileCounts[index]++
    }

    Chart {
        id = "resource-type-distribution-size-chart"
        title = "Resource type distribution (size)"
        description = "Shows the accumulated app size for each resource type."
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(
                seriesOf("Download size", downloadSizes),
                seriesOf("Install size", installSizes)
            ),
            chartHeight = 350,
            yAxisFormatter = ::formatSize,
        )
    }
    Chart {
        id = "resource-type-distribution-count-chart"
        title = "Resource type distribution (file count)"
        description = "Shows how many files of a certain resource type are contained in the app."
        config = BarChartConfig(
            chartLabels = labels,
            chartSeries = arrayOf(seriesOf("Files", fileCounts)),
            chartHeight = 350,
        )
    }
}

external interface ChartProps : Props {
   var id: String
   var title: String
   var description: String
   var config: ChartConfig
}

val Chart = FC<ChartProps> { props ->
    Column {
        h4 { +props.title }
        p {
            className = ClassName("text-muted")
            +props.description
        }
        div {
            id = props.id
            useEffect {
                val chart = ApexCharts(document.getElementById(props.id),props.config.getOptions())
                chart.render()
                cleanup {
                    chart.destroy()
                }
            }
        }
    }
}
