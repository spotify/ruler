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

package com.spotify.ruler.plugin

import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.plugin.apk.ApkCreator
import com.spotify.ruler.plugin.apk.ApkParser
import com.spotify.ruler.plugin.apk.ApkSanitizer
import com.spotify.ruler.plugin.attribution.Attributor
import com.spotify.ruler.plugin.common.ClassNameSanitizer
import com.spotify.ruler.plugin.dependency.DependencyComponent
import com.spotify.ruler.plugin.dependency.DependencyParser
import com.spotify.ruler.plugin.dependency.DependencySanitizer
import com.spotify.ruler.plugin.models.AppInfo
import com.spotify.ruler.plugin.models.DeviceSpec
import com.spotify.ruler.plugin.ownership.OwnershipFileParser
import com.spotify.ruler.plugin.ownership.OwnershipInfo
import com.spotify.ruler.plugin.report.HtmlReporter
import com.spotify.ruler.plugin.report.JsonReporter
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class RulerTask : DefaultTask() {

    @get:Input
    abstract val appInfo: Property<AppInfo>

    @get:Input
    abstract val deviceSpec: Property<DeviceSpec>

    @get:InputFile
    abstract val bundleFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    abstract val mappingFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    abstract val ownershipFile: RegularFileProperty

    @get:Input
    abstract val defaultOwner: Property<String>

    @get:OutputDirectory
    abstract val workingDir: DirectoryProperty

    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    @TaskAction
    fun analyze() {
        val getComponents = { project: Project, splitApks: Map<String, List<File>> ->
            val files = getFilesFromSplitApks(project, splitApks) // Get all relevant files from the provided bundle
            val dependencies = getDependencies(project) // Get all entries from all dependencies
            // Attribute bundle entries and group into components
            val attributor = Attributor(DependencyComponent(project.path, ComponentType.INTERNAL))

            attributor.attribute(files, dependencies)
        }

        val splitApks = createSplitApksFromBundle()
        val components = getComponents(project, splitApks)
        val dfmComponents = HashMap<DependencyComponent, List<AppFile>>()
        appInfo.get().dynamicFeatures.forEach {
            dfmComponents += getComponents(project.rootProject.project(it), splitApks)
        }

        val ownershipInfo = getOwnershipInfo() // Get ownership information for all components
        generateReports(components, dfmComponents, ownershipInfo)
    }

    private fun createSplitApksFromBundle(): Map<String, List<File>> {
        val apkCreator = ApkCreator(project.rootDir)
        return apkCreator.createSplitApks(bundleFile.asFile.get(), deviceSpec.get(), workingDir.asFile.get())
    }

    private fun getFilesFromSplitApks(specifiedProject: Project, splitApks: Map<String, List<File>>): List<AppFile> {
        val apks = splitApks[if (specifiedProject == project) {
            "base"
        } else {
            specifiedProject.name
        }] ?: return emptyList()
        val apkParser = ApkParser()
        val entries = apks.flatMap(apkParser::parse)

        val classNameSanitizer = ClassNameSanitizer(mappingFile.asFile.orNull)
        val apkSanitizer = ApkSanitizer(classNameSanitizer)
        return apkSanitizer.sanitize(entries)
    }

    private fun getDependencies(specifiedProject: Project): Map<String, List<DependencyComponent>> {
        val dependencyParser = DependencyParser()
        val entries = dependencyParser.parse(specifiedProject, appInfo.get())

        val classNameSanitizer = ClassNameSanitizer(mappingFile.asFile.orNull)
        val dependencySanitizer = DependencySanitizer(classNameSanitizer)
        return dependencySanitizer.sanitize(entries)
    }

    private fun getOwnershipInfo(): OwnershipInfo? {
        val ownershipFile = ownershipFile.asFile.orNull ?: return null
        val ownershipFileParser = OwnershipFileParser()
        val ownershipEntries = ownershipFileParser.parse(ownershipFile)

        return OwnershipInfo(ownershipEntries, defaultOwner.get())
    }

    private fun generateReports(components: Map<DependencyComponent, List<AppFile>>, dfmComponents: Map<DependencyComponent, List<AppFile>>, ownershipInfo: OwnershipInfo?) {
        val jsonReporter = JsonReporter()
        val jsonReport = jsonReporter.generateReport(appInfo.get(), components, dfmComponents, ownershipInfo, reportDir.asFile.get())
        project.logger.lifecycle("Wrote JSON report to ${jsonReport.toPath().toUri()}")

        val htmlReporter = HtmlReporter()
        val htmlReport = htmlReporter.generateReport(jsonReport.readText(), reportDir.asFile.get())
        project.logger.lifecycle("Wrote HTML report to ${htmlReport.toPath().toUri()}")
    }
}
