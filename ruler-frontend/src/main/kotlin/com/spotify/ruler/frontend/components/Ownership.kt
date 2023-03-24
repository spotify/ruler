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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.spotify.ruler.frontend.binding.NumberFormatter
import com.spotify.ruler.frontend.chart.BarChartConfig
import com.spotify.ruler.frontend.chart.seriesOf
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.Measurable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

const val PAGE_SIZE = 10

@Composable
fun Ownership(
    components: List<AppComponent>,
    hasFileLevelInfo: Boolean,
    sizeType: Measurable.SizeType
) {
    ComponentOwnershipOverview(
        components = components
    )
    ComponentOwnershipPerTeam(
        components = components,
        hasFileLevelInfo = hasFileLevelInfo,
        sizeType = sizeType,
    )
}

@Composable
fun ComponentOwnershipOverview(components: List<AppComponent>) {
    val sizes = getSizesByOwner(components)
    val sorted = sizes.entries.sortedByDescending { (_, measurable) -> measurable.downloadSize }
    val owners = sorted.map { (owner, _) -> owner }
    val downloadSizes = sorted.map { (_, measurable) -> measurable.downloadSize }
    val installSizes = sorted.map { (_, measurable) -> measurable.installSize }

    PagedContent(
        itemCount = owners.size,
        pageSize = PAGE_SIZE,
        content = { pageStartIndex, pageEndIndex ->
            println("rendering content $pageStartIndex")
            Chart(
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
        })

}

@Composable
fun ComponentOwnershipPerTeam(
    components: List<AppComponent>,
    hasFileLevelInfo: Boolean,
    sizeType: Measurable.SizeType
) {
    val files: List<AppFile>? = when(hasFileLevelInfo) {
        true -> components.mapNotNull(AppComponent::files).flatten()
        false -> null
    }

    val owners: List<String> = when(hasFileLevelInfo) {
        true -> files?.mapNotNull(AppFile::owner) ?: emptyList()
        false -> components.mapNotNull(AppComponent::owner)
    }.distinct().sorted()

    var selectedOwner by remember { mutableStateOf(owners.first()) }

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

    val downloadSize: Long = when(ownedFiles) {
        null -> ownedComponents.sumOf(AppComponent::downloadSize)
        else -> ownedFiles.sumOf(AppFile::downloadSize)
    }

    val installSize = when(ownedFiles) {
        null -> ownedComponents.sumOf(AppComponent::installSize)
        else -> ownedFiles.sumOf(AppFile::installSize)
    }

    H4(attrs = {
        classes("mb-3", "mt-4")
    }) {
        Text("Components and files grouped by owner")
    }
    Dropdown(options = owners, id = "owner-dropdown") { owner -> selectedOwner = owner }
    Div(attrs = {
        classes("row", "mt-4", "mb-4")
    }) {
        HighlightedValue(value = ownedComponents.size, label = "Component(s)")
        ownedFiles?.size?.let { HighlightedValue( value = it,label = "File(s)") }
        HighlightedValue(value = downloadSize, label = "Download size", formatter = ::formatSize)
        HighlightedValue(value = installSize, label = "Install size", formatter = ::formatSize)
    }
    ContainerList(containers = processedComponents,  sizeType = sizeType)
}


@Composable
fun HighlightedValue(
     value: Number,
     label: String,
     formatter: NumberFormatter = Number::toString
) {
    Div(attrs = {
        classes("col", "text-center")
    }) {
        H4 { Text(formatter.invoke(value)) }
        Span(attrs = {
            classes("text-muted", "m-0")
        }) {
            Text(label)
        }
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
