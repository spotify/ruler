/*
* Copyright 2023 Spotify AB
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

package com.spotify.ruler.common

import com.spotify.ruler.common.apk.ApkParser
import com.spotify.ruler.common.apk.ApkSanitizer
import com.spotify.ruler.common.attribution.Attributor
import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.common.models.RulerConfig
import com.spotify.ruler.common.ownership.OwnershipFileParser
import com.spotify.ruler.common.ownership.OwnershipInfo
import com.spotify.ruler.common.report.HtmlReporter
import com.spotify.ruler.common.report.JsonReporter
import com.spotify.ruler.common.sanitizer.ClassNameSanitizer
import com.spotify.ruler.common.sanitizer.ResourceNameSanitizer
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import java.io.File

const val FEATURE_NAME = "base"

interface BaseRulerTask {

    fun print(content: String)
    fun provideMappingFile(): File?
    fun provideResourceMappingFile(): File?
    fun rulerConfig(): RulerConfig
    fun provideUnstrippedLibraryFiles(): List<File>
    private val rulerConfig: RulerConfig
        get() = rulerConfig()

    fun provideDependencies(): Map<String, List<DependencyComponent>>

    fun run() {
        val files = getFilesFromBundle() // Get all relevant files from the provided bundle
        val dependencies = provideDependencies() + mapOf(
            "kotlin" to listOf(DependencyComponent("kotlin", ComponentType.INTERNAL))
        ) // Get all entries from all dependencies
        // Split main APK bundle entries and dynamic feature module entries
        val mainFiles = files.getValue(FEATURE_NAME)
        val featureFiles = files.filter { (feature, _) -> feature != FEATURE_NAME }

        // The default component in Gradle is a "fake" application component.
        // In Bazel we already have an application component based on the target name of the app,
        // which we can reuse
        val defaultComponent = dependencies.values.flatten()
            .firstOrNull { it.name == rulerConfig.projectPath }
            ?: DependencyComponent(rulerConfig.projectPath, ComponentType.INTERNAL)

        // Attribute main APK bundle entries and group into components
        val attributor =
            Attributor(defaultComponent)
        val components = attributor.attribute(mainFiles, dependencies)

        val ownershipInfo = getOwnershipInfo() // Get ownership information for all components
        generateReports(components, featureFiles, ownershipInfo)
    }

    private fun getFilesFromBundle(): Map<String, List<AppFile>> {
        val apkParser = ApkParser(provideUnstrippedLibraryFiles())
        val classNameSanitizer = ClassNameSanitizer(provideMappingFile())
        val resourceNameSanitizer = ResourceNameSanitizer(provideResourceMappingFile())
        val apkSanitizer = ApkSanitizer(classNameSanitizer, resourceNameSanitizer)

        return rulerConfig().apkFilesMap.mapValues { (_, apks) ->
            val entries = apks.flatMap(apkParser::parse)
            apkSanitizer.sanitize(entries)
        }
    }

    private fun getOwnershipInfo(): OwnershipInfo? {
        val ownershipFile = rulerConfig.ownershipFile ?: return null
        val ownershipFileParser = OwnershipFileParser()
        val ownershipEntries = ownershipFileParser.parse(ownershipFile)

        return OwnershipInfo(ownershipEntries, rulerConfig.defaultOwner)
    }

    private fun generateReports(
        components: Map<DependencyComponent, List<AppFile>>,
        features: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
    ) {
        val reportDir = rulerConfig.reportDir

        val jsonReporter = JsonReporter()
        val jsonReport = jsonReporter.generateReport(
            rulerConfig.appInfo,
            components,
            features,
            ownershipInfo,
            reportDir,
            rulerConfig.omitFileBreakdown
        )

        print("Wrote JSON report to ${jsonReport.toPath().toUri()}")

        val htmlReporter = HtmlReporter()
        val htmlReport = htmlReporter.generateReport(jsonReport.readText(), reportDir)
        print("Wrote HTML report to ${htmlReport.toPath().toUri()}")
    }
}
