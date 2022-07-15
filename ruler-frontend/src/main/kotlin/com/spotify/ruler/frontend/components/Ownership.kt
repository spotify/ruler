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
import com.spotify.ruler.frontend.binding.NumberFormatter
import com.spotify.ruler.frontend.chart.BarChartConfig
import com.spotify.ruler.frontend.chart.seriesOf
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.Measurable
import react.RBuilder
import react.dom.div
import react.dom.h4
import react.dom.span
import react.useState

const val PAGE_SIZE = 10

@RFunction
fun RBuilder.ownership(components: List<AppComponent>, sizeType: Measurable.SizeType) {
    componentOwnershipOverview(components)
    componentOwnershipPerTeam(components, sizeType)
}

@RFunction
fun RBuilder.componentOwnershipOverview(components: List<AppComponent>) {
    val sizes = mutableMapOf<String, Measurable.Mutable>()
    components.flatMap(AppComponent::files).forEach { file ->
        val owner = file.owner ?: return@forEach
        val current = sizes.getOrPut(owner) { Measurable.Mutable(0, 0) }
        current.downloadSize += file.downloadSize
        current.installSize += file.installSize
    }

    val sorted = sizes.entries.sortedByDescending { (_, measurable) -> measurable.downloadSize }
    val owners = sorted.map { (owner, _) -> owner }
    val downloadSizes = sorted.map { (_, measurable) -> measurable.downloadSize }
    val installSizes = sorted.map { (_, measurable) -> measurable.installSize }

    pagedContent(owners.size, PAGE_SIZE) { pageStartIndex, pageEndIndex ->
        chart(
            id = "owner-chart",
            title = "Ownership overview",
            description = "Shows how much of the overall app size is owned by each owner.",
            config = BarChartConfig(
                chartLabels = owners.subList(pageStartIndex, pageEndIndex).toTypedArray(),
                chartSeries = arrayOf(
                    seriesOf("Download size", downloadSizes.subList(pageStartIndex, pageEndIndex).toLongArray()),
                    seriesOf("Install size", installSizes.subList(pageStartIndex, pageEndIndex).toLongArray()),
                ),
                chartHeight = 400,
                yAxisFormatter = ::formatSize,
                chartSeriesTotals = longArrayOf(downloadSizes.sum(), installSizes.sum()),
            )
        )
    }
}

@RFunction
fun RBuilder.componentOwnershipPerTeam(components: List<AppComponent>, sizeType: Measurable.SizeType) {
    val files = components.flatMap(AppComponent::files)
    val owners = files.mapNotNull(AppFile::owner).distinct().sorted()
    var selectedOwner by useState(owners.first())

    val ownedComponents = components.filter { component -> component.owner == selectedOwner }
    val ownedFiles = files.filter { file -> file.owner == selectedOwner }

    val remainingOwnedFiles = ownedFiles.toMutableSet()
    val processedComponents = ownedComponents.map { component ->
        val ownedFilesFromComponent = component.files.filter { file -> file.owner == selectedOwner }
        remainingOwnedFiles.removeAll(ownedFilesFromComponent)
        component.copy(
            downloadSize = ownedFilesFromComponent.sumOf(AppFile::downloadSize),
            installSize = ownedFilesFromComponent.sumOf(AppFile::installSize),
            files = ownedFilesFromComponent,
        )
    }.toMutableList()

    // Group together all owned files which belong to components not owned by the currently selected owner
    if (remainingOwnedFiles.isNotEmpty()) {
        processedComponents += AppComponent(
            name = "Other owned files",
            type = ComponentType.INTERNAL,
            downloadSize = remainingOwnedFiles.sumOf(AppFile::downloadSize),
            installSize = remainingOwnedFiles.sumOf(AppFile::installSize),
            files = remainingOwnedFiles.toList(),
            owner = selectedOwner,
        )
    }

    h4(classes = "mb-3 mt-4") { +"Components and files grouped by owner" }
    dropdown(owners, "owner-dropdown") { owner -> selectedOwner = owner }
    div(classes = "row mt-4 mb-4") {
        highlightedValue(ownedComponents.size, "Component(s)")
        highlightedValue(ownedFiles.size, "File(s)")
        highlightedValue(ownedFiles.sumOf(AppFile::downloadSize), "Download size", ::formatSize)
        highlightedValue(ownedFiles.sumOf(AppFile::installSize), "Install size", ::formatSize)
    }
    containerList(processedComponents, sizeType)
}

@RFunction
fun RBuilder.highlightedValue(value: Number, label: String, formatter: NumberFormatter = Number::toString) {
    div(classes = "col text-center") {
        h4 { +formatter.invoke(value) }
        span(classes = "text-muted m-0") { +label }
    }
}
