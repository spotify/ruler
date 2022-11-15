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

package com.spotify.ruler.plugin.report

import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.Insights
import com.spotify.ruler.models.Measurable
import com.spotify.ruler.models.MutableTypeInsights
import com.spotify.ruler.models.OwnedSize
import com.spotify.ruler.models.Owner
import com.spotify.ruler.models.OwnershipOverview
import com.spotify.ruler.models.ResourceType
import com.spotify.ruler.models.TypeInsights
import com.spotify.ruler.plugin.dependency.DependencyComponent
import com.spotify.ruler.plugin.models.AppInfo
import com.spotify.ruler.plugin.ownership.OwnershipInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Responsible for generating JSON reports. */
class JsonReporter {
    private val comparator = compareBy(Measurable::downloadSize).thenBy(Measurable::installSize)

    /**
     * Generates a JSON report. Entries will be sorted in descending order based on their size.
     *
     * @param appInfo General info about the analyzed app.
     * @param components Map of app component names to their respective files
     * @param ownershipInfo Optional info about the owners of components.
     * @param targetDir Directory where the generated report will be located
     * @return Generated JSON report file
     */
    fun generateReport(
        appInfo: AppInfo,
        components: Map<DependencyComponent, List<AppFile>>,
        features: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
        shouldExcludeFileListing: Boolean,
        targetDir: File
    ): File {
        val appComponents = getAppComponents(components, ownershipInfo)
        val report = AppReport(
            name = appInfo.applicationId,
            version = appInfo.versionName,
            variant = appInfo.variantName,
            components = appComponents.excludeComponentFilesIfNeeded(shouldExcludeFileListing),
            dynamicFeatures = getDynamicFeatures(features, ownershipInfo)
                .excludeDyamicFeatureFilesIfNeeded(shouldExcludeFileListing),
            insights = getInsights(components, appComponents),
            ownershipOverview = ownershipInfo?.let { getOwnershipOverview(appComponents) },
        )

        val format = Json { prettyPrint = true }
        val reportFile = targetDir.resolve("report.json")
        reportFile.writeText(format.encodeToString(report))
        return reportFile
    }

    private fun getAppComponents(
        components: Map<DependencyComponent, List<AppFile>>,
        ownershipInfo: OwnershipInfo?
    ): List<AppComponent> =
        components.map { (component, files) ->
            val componentFiles = files.map { file ->
                AppFile(
                    name = file.name,
                    type = file.type,
                    downloadSize = file.downloadSize,
                    installSize = file.installSize,
                    owner = ownershipInfo?.getOwner(file.name, component.name, component.type),
                    resourceType = file.resourceType,
                )
            }.sortedWith(comparator.reversed())
            AppComponent(
                name = component.name,
                type = component.type,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = ownershipInfo?.let {
                    getOwner(ownershipInfo.getOwner(component.name, component.type), componentFiles)
                },
                files = componentFiles
            )
        }.sortedWith(comparator.reversed())

    private fun getDynamicFeatures(
        features: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?
    ): List<DynamicFeature> =
        features.map { (feature, files) ->
            val dynamicFeatureFiles = files.map { file ->
                AppFile(
                    name = file.name,
                    type = file.type,
                    downloadSize = file.downloadSize,
                    installSize = file.installSize,
                    owner = ownershipInfo?.getOwner(file.name, feature),
                    resourceType = file.resourceType,
                )
            }.sortedWith(comparator.reversed())
            DynamicFeature(
                name = feature,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = ownershipInfo?.let { getOwner(ownershipInfo.getOwner(feature), dynamicFeatureFiles) },
                files = dynamicFeatureFiles
            )
        }.sortedWith(comparator.reversed())

    private fun getOwner(
        componentOwnerName: String,
        files: List<AppFile>,
    ): Owner {
        var ownedDownloadSize = 0L
        var ownedInstallSize = 0L
        files.filter { it.owner == componentOwnerName }.forEach { ownedFile ->
            ownedDownloadSize += ownedFile.downloadSize
            ownedInstallSize += ownedFile.installSize
        }

        return Owner(
            name = componentOwnerName,
            ownedSize = OwnedSize(
                downloadSize = ownedDownloadSize,
                installSize = ownedInstallSize,
            )
        )
    }

    private fun getInsights(
        components: Map<DependencyComponent, List<AppFile>>,
        appComponents: List<AppComponent>,
    ): Insights =
        Insights(
            appDownloadSize = components.values.flatten().sumOf(AppFile::downloadSize),
            appInstallSize = components.values.flatten().sumOf(AppFile::installSize),
            fileTypeInsights = getFileTypeInsights(appComponents),
            componentTypeInsights = getComponentTypeInsights(appComponents),
            resourcesTypeInsights = getResourcesTypeInsights(appComponents),
        )

    private fun getFileTypeInsights(components: List<AppComponent>): Map<FileType, TypeInsights> =
        getTypeInsights(
            items = components.flatMap(AppComponent::files),
            getKey = { type }
        )

    private fun getComponentTypeInsights(components: List<AppComponent>): Map<ComponentType, TypeInsights> =
        getTypeInsights(
            items = components,
            getKey = { type }
        )

    private fun getResourcesTypeInsights(components: List<AppComponent>): Map<ResourceType, TypeInsights> =
        getTypeInsights(
            items = components.flatMap(AppComponent::files).filter { it.resourceType != null },
            getKey = { resourceType!! }
        )

    private inline fun <K, T : Measurable> getTypeInsights(
        items: List<T>,
        getKey: T.() -> K
    ): Map<K, TypeInsights> {
        val typeInsights = mutableMapOf<K, MutableTypeInsights>()

        items.forEach { item ->
            val key = item.getKey()
            val currentInsights = typeInsights.getOrPut(key) {
                MutableTypeInsights(
                    downloadSize = 0L,
                    installSize = 0L,
                    count = 0L,
                )
            }
            currentInsights.downloadSize += item.getSize(Measurable.SizeType.DOWNLOAD)
            currentInsights.installSize += item.getSize(Measurable.SizeType.INSTALL)
            currentInsights.count++
        }

        return typeInsights.asSequence()
            .map {
                val (type, insights) = it
                type to TypeInsights(
                    downloadSize = insights.downloadSize,
                    installSize = insights.installSize,
                    count = insights.count
                )
            }.toMap()
    }

    private fun getOwnershipOverview(appComponents: List<AppComponent>): Map<String, OwnershipOverview> {
        val overview = mutableMapOf<String, OwnershipOverview>()

        appComponents.forEach { component ->
            component.files.forEach { file ->
                file.owner?.let { fileOwner ->
                    val current = overview.getOrPut(fileOwner) {
                        OwnershipOverview(
                            totalDownloadSize = 0,
                            totalInstallSize = 0,
                            filesCount = 0,
                            filesFromNotOwnedComponentsDownloadSize = 0,
                            filesFromNotOwnedComponentsInstallSize = 0,
                        )
                    }
                    current.totalDownloadSize += file.downloadSize
                    current.totalInstallSize += file.installSize
                    current.filesCount++
                    if (fileOwner != component.owner?.name) {
                        current.filesFromNotOwnedComponentsDownloadSize += file.downloadSize
                        current.filesFromNotOwnedComponentsInstallSize += file.installSize
                    }
                }
            }
        }

        return overview
    }

    private fun List<AppComponent>.excludeComponentFilesIfNeeded(
        shouldExcludeFileListing: Boolean
    ): List<AppComponent> =
        if (shouldExcludeFileListing) {
            map { it.copy(files = emptyList()) }
        } else {
            this
        }

    private fun List<DynamicFeature>.excludeDyamicFeatureFilesIfNeeded(
        shouldExcludeFileListing: Boolean
    ): List<DynamicFeature> =
        if (shouldExcludeFileListing) {
            map { it.copy(files = emptyList()) }
        } else {
            this
        }
}
