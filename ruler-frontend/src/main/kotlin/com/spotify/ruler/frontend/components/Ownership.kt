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
import com.spotify.ruler.models.FileContainer
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.Measurable
import react.RBuilder
import react.dom.div
import react.dom.h4
import react.dom.span
import react.useState
import react.table.columns
import react.table.useTable
import react.table.TableInstance
import kotlinx.js.jso
import react.table.usePagination

const val PAGE_SIZE = 10
private val COMPONENTS_COLUMNS = columns<AppComponent> {
    column<String> {
        accessorFunction = { it.name }
        id = "name"
    }

    column<String?> {
        accessorFunction = { it.owner }
        id = "owner"
    }

    column<Long> {
        accessorFunction = { it.downloadSize }
        id = "downloadSize"
    }

    column<Long> {
        accessorFunction = { it.installSize }
        id = "installSize"
    }
}

@RFunction
fun RBuilder.ownership(components: List<AppComponent>, hasFileLevelInfo: Boolean, sizeType: Measurable.SizeType) {
    componentOwnershipOverview(components)
    componentOwnershipPerTeam(components, hasFileLevelInfo, sizeType)
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
fun RBuilder.componentOwnershipPerTeam(
    components: List<AppComponent>,
    hasFileLevelInfo: Boolean,
    sizeType: Measurable.SizeType,
) {
    val files: List<AppFile>?
    var owners: List<String>
    if (hasFileLevelInfo) {
        files = components.mapNotNull(AppComponent::files).flatten()
        owners = files.mapNotNull(AppFile::owner)
    } else {
        files = null
        owners = components.mapNotNull(AppComponent::owner)
    }
    owners = owners.distinct().sorted()
    var selectedOwner by useState(owners.first())

    val ownedComponents = components.filter { component -> component.owner == selectedOwner }
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

    val downloadSize: Long
    val installSize: Long
    if (ownedFiles == null) {
        // If there is no file-level ownership info, use component-level ownership info
        downloadSize = ownedComponents.sumOf(AppComponent::downloadSize)
        installSize = ownedComponents.sumOf(AppComponent::installSize)
    } else {
        // Otherwise rely on file-level ownership info
        downloadSize = ownedFiles.sumOf(AppFile::downloadSize)
        installSize = ownedFiles.sumOf(AppFile::installSize)
    }

    h4(classes = "mb-3 mt-4") { +"Components and files grouped by owner" }
    dropdown(owners, "owner-dropdown") { owner -> selectedOwner = owner }
    div(classes = "row mt-4 mb-4") {
        highlightedValue(ownedComponents.size, "Component(s)")
        ownedFiles?.size?.let { highlightedValue(it, "File(s)") }
        highlightedValue(downloadSize, "Download size", ::formatSize)
        highlightedValue(installSize, "Install size", ::formatSize)
    }

    val table = useTable<AppComponent>(
        options = jso {
            data = processedComponents.toTypedArray()
            columns = COMPONENTS_COLUMNS
            // initialState = jso {
            //     pageSize = 50
            // },
            // usePagination
        }
    );

    table.getTableProps()
    table.getTableBodyProps()

    containerList(table  as TableInstance<FileContainer>, sizeType)
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

    components.forEach { component ->
        // If there is no file-level ownership info, use component-level ownership info
        if (component.files == null) {
            val owner = component.owner ?: return@forEach
            val current = sizes.getOrPut(owner) { Measurable.Mutable(0, 0) }
            current.downloadSize += component.downloadSize
            current.installSize += component.installSize
            return@forEach
        }

        // Otherwise rely on file-level ownership info
        component.files?.forEach fileLevelLoop@ { file ->
            val owner = file.owner ?: return@fileLevelLoop
            val current = sizes.getOrPut(owner) { Measurable.Mutable(0, 0) }
            current.downloadSize += file.downloadSize
            current.installSize += file.installSize
        }
    }

    return sizes
}
