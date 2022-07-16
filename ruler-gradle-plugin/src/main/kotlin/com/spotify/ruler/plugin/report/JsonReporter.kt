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
import com.spotify.ruler.models.Measurable
import com.spotify.ruler.plugin.dependency.DependencyComponent
import com.spotify.ruler.plugin.models.AppInfo
import com.spotify.ruler.plugin.ownership.OwnershipInfo
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Responsible for generating JSON reports. */
class JsonReporter {
    private val comparator = compareBy(Measurable::downloadSize).thenBy(Measurable::installSize)

    /**
     * Generates a JSON report. Entries will be sorted in descending order based on their size.
     *
     * @param appInfo General info about the analyzed app.
     * @param appComponents Map of app component names to their respective files
     * @param dfmComponents Map of dynamic feature component names to their respective files
     * @param ownershipInfo Optional info about the owners of components.
     * @param targetDir Directory where the generated report will be located
     * @return Generated JSON report file
     */
    fun generateReport(
        appInfo: AppInfo,
        appComponents: Map<DependencyComponent, List<AppFile>>,
        dfmComponents: Map<DependencyComponent, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
        targetDir: File
    ): File {
        val getComponents = { components: Map<DependencyComponent, List<AppFile>> ->
            components.map { (component, files) ->
                AppComponent(
                    name = component.name,
                    type = component.type,
                    downloadSize = files.sumOf(AppFile::downloadSize),
                    installSize = files.sumOf(AppFile::installSize),
                    owner = ownershipInfo?.getOwner(component.name, component.type),
                    files = files.map { file ->
                        AppFile(
                            name = file.name,
                            type = file.type,
                            downloadSize = file.downloadSize,
                            installSize = file.installSize,
                            owner = ownershipInfo?.getOwner(file.name, component.name, component.type),
                        )
                    }.sortedWith(comparator.reversed())
                )
            }.sortedWith(comparator.reversed())
        }
        val report = AppReport(
            name = appInfo.applicationId,
            version = appInfo.versionName,
            variant = appInfo.variantName,
            downloadSize = appComponents.getDownloadSize() + dfmComponents.getDownloadSize(),
            installSize = appComponents.getInstallSize() + dfmComponents.getInstallSize(),
            components = getComponents(appComponents),
            dynamicFeatureComponents = getComponents(dfmComponents)
        )

        val format = Json { prettyPrint = true }
        val reportFile = targetDir.resolve("report.json")
        reportFile.writeText(format.encodeToString(report))
        return reportFile
    }

    private fun Map<DependencyComponent, List<AppFile>>.getDownloadSize(): Long {
        return values.flatten().sumOf(AppFile::downloadSize)
    }

    private fun Map<DependencyComponent, List<AppFile>>.getInstallSize(): Long {
        return values.flatten().sumOf(AppFile::installSize)
    }
}
