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
import com.spotify.ruler.frontend.filesWereOmitted
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
    if (components.filesWereOmitted()) {
        componentOwnershipPerTeamWithoutFilesBreakdown(components, sizeType)
    } else {
        componentOwnershipPerTeamWithFilesBreakdown(components, sizeType)
    }
}

@RFunction
fun RBuilder.componentOwnershipOverview(components: List<AppComponent>) {
    val sizes = getSizesByOwner(components)
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
fun RBuilder.componentOwnershipPerTeamWithoutFilesBreakdown(
    components: List<AppComponent>,
    sizeType: Measurable.SizeType,
) {
    val owners = components.mapNotNull(AppComponent::owner).distinct().sorted()
    var selectedOwner by useState(owners.first())

    val ownedComponents = components.filter { component -> component.owner == selectedOwner }

    componentOwnershipPerTeam(
        onSelectedOwnerUpdated = { owner -> selectedOwner = owner },
        owners = owners,
        ownedComponentsCount = ownedComponents.size,
        ownedFilesCount = null,
        downloadSize = ownedComponents.sumOf(AppComponent::downloadSize),
        installSize = ownedComponents.sumOf(AppComponent::installSize),
        processedComponents = ownedComponents,
        sizeType = sizeType,
    )
}

@RFunction
fun RBuilder.componentOwnershipPerTeamWithFilesBreakdown(
    components: List<AppComponent>,
    sizeType: Measurable.SizeType,
) {
    val files = components.flatMap { it.files ?: emptyList() }
    val owners = files.mapNotNull(AppFile::owner).distinct().sorted()
    var selectedOwner by useState(owners.first())

    val ownedComponents = components.filter { component -> component.owner == selectedOwner }
    val ownedFiles = files.filter { file -> file.owner == selectedOwner }

    val remainingOwnedFiles = ownedFiles.toMutableSet()
    val processedComponents = ownedComponents.map { component ->
        val ownedFilesFromComponent = component.files?.filter { file -> file.owner == selectedOwner } ?: emptyList()
        remainingOwnedFiles.removeAll(ownedFilesFromComponent.toSet())
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

    componentOwnershipPerTeam(
        onSelectedOwnerUpdated = { owner -> selectedOwner = owner },
        owners = owners,
        ownedComponentsCount = ownedComponents.size,
        ownedFilesCount = ownedFiles.size,
        downloadSize = ownedFiles.sumOf(AppFile::downloadSize),
        installSize = ownedFiles.sumOf(AppFile::installSize),
        processedComponents = processedComponents,
        sizeType = sizeType,
    )
}

@RFunction
fun RBuilder.componentOwnershipPerTeam(
    onSelectedOwnerUpdated: (owner: String) -> Unit,
    owners: List<String>,
    ownedComponentsCount: Int,
    ownedFilesCount: Int?,
    downloadSize: Long,
    installSize: Long,
    processedComponents: List<AppComponent>,
    sizeType: Measurable.SizeType,
) {
    h4(classes = "mb-3 mt-4") { +"Components and files grouped by owner" }
    dropdown(owners, "owner-dropdown", onSelectedOwnerUpdated)
    div(classes = "row mt-4 mb-4") {
        highlightedValue(ownedComponentsCount, "Component(s)")
        ownedFilesCount?.let { highlightedValue(it, "File(s)") }
        highlightedValue(downloadSize, "Download size", ::formatSize)
        highlightedValue(installSize, "Install size", ::formatSize)
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

private fun getSizesByOwner(components: List<AppComponent>): Map<String, Measurable> {
    val sizes = mutableMapOf<String, Measurable.Mutable>()

    val omitFileOwnership = components.filesWereOmitted()
    if (omitFileOwnership) {
        sizes.populateWithSizesByOwner(
            getOwner = { component -> component.owner },
            measurables = components,
        )
    } else {
        sizes.populateWithSizesByOwner(
            getOwner = { file -> file.owner },
            measurables = components.flatMap { it.files ?: emptyList() },
        )
    }

    return sizes
}

private inline fun <T : Measurable> MutableMap<String, Measurable.Mutable>.populateWithSizesByOwner(
    getOwner: (T) -> String?,
    measurables: List<T>,
) {
    measurables.forEach { measurable ->
        val owner = getOwner(measurable) ?: return@forEach
        val current = getOrPut(owner) { Measurable.Mutable(0, 0) }
        current.downloadSize += measurable.downloadSize
        current.installSize += measurable.installSize
    }
}
