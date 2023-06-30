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

import com.spotify.ruler.frontend.binding.NumberFormatter
import com.spotify.ruler.frontend.chart.BarChartConfig
import com.spotify.ruler.frontend.chart.seriesOf
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.Measurable
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.span
import react.useState
import web.cssom.ClassName
import kotlin.math.ceil
import kotlin.math.min

const val PAGE_SIZE = 10


external interface OwnershipProps : Props {
    var components: List<AppComponent>
    var hasFileLevelInfo: Boolean
    var sizeType: Measurable.SizeType
}

val Ownership = FC<OwnershipProps> { props ->
    ComponentOwnershipOverview {
        components = props.components
    }
    ComponentOwnershipPerTeam {
        components = props.components
        hasFileLevelInfo = props.hasFileLevelInfo
        sizeType = props.sizeType
    }
}

external interface ComponentOwnershipOverviewProps : Props {
    var components: List<AppComponent>
}

val ComponentOwnershipOverview = FC<ComponentOwnershipOverviewProps>  { props ->
    val sizes = getSizesByOwner(props.components)
    val sorted = sizes.entries.sortedByDescending { (_, measurable) -> measurable.downloadSize }
    val owners = sorted.map { (owner, _) -> owner }
    val downloadSizes = sorted.map { (_, measurable) -> measurable.downloadSize }
    val installSizes = sorted.map { (_, measurable) -> measurable.installSize }

    val pageCount = ceil(owners.size / PAGE_SIZE.toFloat()).toInt()
    var activePage by useState(1)

    val pageStartIndex = PAGE_SIZE * (activePage - 1)
    val pageEndIndex = min(pageStartIndex + PAGE_SIZE, owners.size)

    Chart {
        id = "owner-chart"
        title = "Ownership overview"
        description = "Shows how much of the overall app size is owned by each owner."
        config = BarChartConfig(
            chartLabels = owners.subList(pageStartIndex, pageEndIndex).toTypedArray(),
            chartSeries = arrayOf(
                seriesOf(
                    "Download size",
                    downloadSizes.subList(pageStartIndex, pageEndIndex).toDoubleArray()
                ),
                seriesOf(
                    "Install size",
                    installSizes.subList(pageStartIndex, pageEndIndex).toDoubleArray()
                ),
            ),
            chartHeight = 400,
            yAxisFormatter = ::formatSize,
            chartSeriesTotals = doubleArrayOf(downloadSizes.sum(), installSizes.sum()),
        )
    }

    PageControl {
        this.pageCount = pageCount
        this.activePage = activePage
        onChangePage = {
            activePage = it
        }
    }
}

external interface ComponentOwnershipPerTeamProps : Props {
    var components: List<AppComponent>
    var hasFileLevelInfo: Boolean
    var sizeType: Measurable.SizeType
}

val ComponentOwnershipPerTeam = FC<ComponentOwnershipPerTeamProps> { props ->
    val files: List<AppFile>?
    var owners: List<String>
    if (props.hasFileLevelInfo) {
        files = props.components.mapNotNull(AppComponent::files).flatten()
        owners = files.mapNotNull(AppFile::owner)
    } else {
        files = null
        owners = props.components.mapNotNull(AppComponent::owner)
    }
    owners = owners.distinct().sorted()
    var selectedOwner by useState(owners.first())

    val ownedComponents = props.components.filter { component -> component.owner == selectedOwner }
    val ownedFiles = files?.filter { file -> file.owner == selectedOwner }

    val remainingOwnedFiles = ownedFiles?.toMutableSet()
    val processedComponents = ownedComponents.map { component ->
        val ownedFilesFromComponent = component.files?.filter { file ->
            file.owner == selectedOwner
        } ?: return@map component

        remainingOwnedFiles?.removeAll(ownedFilesFromComponent.toSet())
        component.copy(
            downloadSize = ownedFilesFromComponent.sumOf(AppFile::downloadSize),
            installSize = ownedFilesFromComponent.sumOf(AppFile::installSize),
            files = ownedFilesFromComponent,
        )
    }.toMutableList()

    // Group together all owned files which belong to components not owned by the currently selected owner
    if (!remainingOwnedFiles.isNullOrEmpty()) {
        processedComponents += AppComponent(
            name = "Other owned files",
            type = ComponentType.INTERNAL,
            downloadSize = remainingOwnedFiles.sumOf(AppFile::downloadSize),
            installSize = remainingOwnedFiles.sumOf(AppFile::installSize),
            files = remainingOwnedFiles.toList(),
            owner = selectedOwner,
        )
    }

    val downloadSize: Double
    val installSize: Double
    if (ownedFiles == null) {
        // If there is no file-level ownership info, use component-level ownership info
        downloadSize = ownedComponents.sumOf(AppComponent::downloadSize)
        installSize = ownedComponents.sumOf(AppComponent::installSize)
    } else {
        // Otherwise rely on file-level ownership info
        downloadSize = ownedFiles.sumOf(AppFile::downloadSize)
        installSize = ownedFiles.sumOf(AppFile::installSize)
    }

    h4 {
        className = ClassName("mb-3 mt-4")
        +"Components and files grouped by owner"
    }
    DropDown {
        options = owners
        id = "owner-dropdown"
        onOptionSelected = { owner -> selectedOwner = owner }
    }
    div {
        className = ClassName("row mt-4 mb-4")
        HighlightedValue {
            value = ownedComponents.size
            label =  "Component(s)"
            formatter = Number::toString
        }
        ownedFiles?.size?.let {
            HighlightedValue {
                value = it
                label =  "File(s)"
                formatter = Number::toString
            }
        }
        HighlightedValue {
            value = downloadSize
            label =  "Download size"
            formatter = ::formatSize
        }
        HighlightedValue {
            value = installSize
            label =  "Install size"
            formatter = ::formatSize
        }
    }

    ContainerList {
        containers = processedComponents
        this@ContainerList.sizeType = props.sizeType
    }
}

external interface HighlightedValueProps : Props {
    var value: Number
    var label: String
    var formatter: NumberFormatter
}

val HighlightedValue = FC<HighlightedValueProps> { props ->
    Column {
        className = ClassName("text-center")
        h4 { +props.formatter.invoke(props.value) }
        span {
            className = ClassName("text-muted m-0")
            +props.label
        }
    }
}


private fun getSizesByOwner(components: List<AppComponent>): Map<String, Measurable> {
    val sizes = mutableMapOf<String, Measurable.Mutable>()

    components.forEach { component ->
        // If there is no file-level ownership info, use component-level ownership info
        if (component.files == null) {
            val owner = component.owner ?: return@forEach
            val current = sizes.getOrPut(owner) { Measurable.Mutable(0.0, 0.0) }
            current.downloadSize += component.downloadSize
            current.installSize += component.installSize
            return@forEach
        }

        // Otherwise rely on file-level ownership info
        component.files?.forEach fileLevelLoop@ { file ->
            val owner = file.owner ?: return@fileLevelLoop
            val current = sizes.getOrPut(owner) { Measurable.Mutable(0.0, 0.0) }
            current.downloadSize += file.downloadSize
            current.installSize += file.installSize
        }
    }

    return sizes
}
