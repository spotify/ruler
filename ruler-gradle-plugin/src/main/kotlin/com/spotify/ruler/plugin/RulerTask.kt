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

import com.android.build.gradle.BaseExtension
import com.spotify.ruler.common.BaseRulerTask
import com.spotify.ruler.common.apk.ApkCreator
import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.common.dependency.DependencySanitizer
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.models.DeviceSpec
import com.spotify.ruler.common.models.RulerConfig
import com.spotify.ruler.common.sanitizer.ClassNameSanitizer
import com.spotify.ruler.plugin.dependency.EntryParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RulerTask : DefaultTask(), BaseRulerTask {

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
    abstract val resourceMappingFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    abstract val ownershipFile: RegularFileProperty

    @get:Input
    abstract val defaultOwner: Property<String>

    @get:Input
    abstract val omitFileBreakdown: Property<Boolean>

    @get:Optional
    @get:InputFiles
    abstract val unstrippedNativeFiles: ListProperty<RegularFile>

    @get:Optional
    @get:InputFile
    abstract val staticDependenciesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val workingDir: DirectoryProperty

    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    @TaskAction
    fun analyze() {
        run()
    }

    private val config by lazy {
        RulerConfig(
            projectPath = project.path,
            apkFilesMap = createApkFile(),
            reportDir = reportDir.asFile.get(),
            ownershipFile = ownershipFile.asFile.orNull,
            staticDependenciesFile = staticDependenciesFile.asFile.orNull,
            appInfo = appInfo.get(),
            deviceSpec = deviceSpec.get(),
            defaultOwner = defaultOwner.get(),
            omitFileBreakdown = omitFileBreakdown.get(),
            additionalEntries = emptyList(),
            ignoredFiles = emptyList()
        )
    }

    override fun rulerConfig(): RulerConfig = config

    override fun provideDependencies(): Map<String, List<DependencyComponent>> {
        val dependencyParser = EntryParser()
        val entries = dependencyParser.parse(project, rulerConfig().appInfo)

        val classNameSanitizer = ClassNameSanitizer(provideMappingFile())
        val dependencySanitizer = DependencySanitizer(classNameSanitizer)
        return dependencySanitizer.sanitize(entries)
    }

    override fun print(content: String) = project.logger.lifecycle(content)
    override fun provideMappingFile(): File? = mappingFile.asFile.orNull
    override fun provideResourceMappingFile(): File? = resourceMappingFile.asFile.orNull
    override fun provideUnstrippedLibraryFiles(): List<File> = unstrippedNativeFiles.get().map {
        it.asFile
    }

    override fun provideBloatyPath() = null

    private fun createApkFile(): Map<String, List<File>> {
        val android = project.extensions.findByName("android") as BaseExtension?
        val apkCreator = ApkCreator(android?.sdkDirectory)

        val apkFile = bundleFile.asFile.get()
        return if (apkFile.extension == "apk") {
            mapOf(ApkCreator.BASE_FEATURE_NAME to listOf(apkFile))
        } else {
            apkCreator.createSplitApks(
                apkFile,
                deviceSpec.get(),
                workingDir.asFile.get()
            )
        }
    }
}
