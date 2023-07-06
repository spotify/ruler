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

import com.spotify.ruler.common.apk.ApkParser
import com.spotify.ruler.common.apk.totalDownloadSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.Measurable
import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.ownership.OwnershipInfo
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
     * @param omitFileBreakdown If true, the list of files for each component and dynamic feature will be omitted
     * @return Generated JSON report file
     */
    @Suppress("LongParameterList")
    fun generateReport(
        appInfo: AppInfo,
        components: Map<DependencyComponent, List<AppFile>>,
        features: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
        targetDir: File,
        omitFileBreakdown: Boolean,
    ): File {
        val report = AppReport(
            name = appInfo.applicationId,
            version = appInfo.versionName,
            variant = appInfo.variantName,
            downloadSize = totalDownloadSize,
            installSize = components.values.flatten().sumOf(AppFile::downloadSize),
            components = components.map { (component, files) ->
                AppComponent(
                    name = component.name + " (${files.count()}) ",
                    type = component.type,
                    downloadSize = files.sumOf(AppFile::downloadSize),
                    installSize = files.sumOf(AppFile::installSize),
                    owner = ownershipInfo?.getOwner(component.name, component.type),
                    files = if (omitFileBreakdown) {
                        null
                    } else {
                        files.map { file ->
                            AppFile(
                                name = file.name,
                                type = file.type,
                                downloadSize = file.downloadSize,
                                installSize = file.installSize,
                                owner = ownershipInfo?.getOwner(file.name, component.name, component.type),
                                resourceType = file.resourceType,
                            )
                        }.sortedWith(comparator.reversed())
                    }
                )
            }.sortedWith(comparator.reversed()),
            dynamicFeatures = features.map { (feature, files) ->
                DynamicFeature(
                    name = feature,
                    downloadSize = files.sumOf(AppFile::downloadSize),
                    installSize = files.sumOf(AppFile::installSize),
                    owner = ownershipInfo?.getOwner(feature),
                    files = if (omitFileBreakdown) {
                        null
                    } else {
                        files.map { file ->
                            AppFile(
                                name = file.name,
                                type = file.type,
                                downloadSize = file.downloadSize,
                                installSize = file.installSize,
                                owner = ownershipInfo?.getOwner(file.name, feature),
                                resourceType = file.resourceType,
                            )
                        }.sortedWith(comparator.reversed())
                    }
                )
            }.sortedWith(comparator.reversed()),
        )

        val format = Json { prettyPrint = false }
        val reportFile = targetDir.resolve("report.json")
        reportFile.writeText(format.encodeToString(report))
        return reportFile
    }
}
