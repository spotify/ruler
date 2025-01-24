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

import com.spotify.ruler.common.BaseRulerTask
import com.spotify.ruler.common.apk.ApkCreator
import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.common.dependency.DependencyEntry
import com.spotify.ruler.common.dependency.DependencySanitizer
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.models.DeviceSpec
import com.spotify.ruler.common.models.RulerConfig
import com.spotify.ruler.common.sanitizer.ClassNameSanitizer
import com.spotify.ruler.common.veritication.VerificationConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class RulerTask : DefaultTask() {

    @get:Input
    abstract val dependencyEntries: MapProperty<String, List<DependencyEntry>>

    @get:Input
    abstract val projectPath: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sdkDirectory: DirectoryProperty

    @get:Input
    abstract val appInfo: Property<AppInfo>

    @get:Input
    abstract val deviceSpec: Property<DeviceSpec>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val bundleFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mappingFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceMappingFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ownershipFile: RegularFileProperty

    @get:Input
    abstract val defaultOwner: Property<String>

    @get:Input
    abstract val omitFileBreakdown: Property<Boolean>

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val unstrippedNativeFiles: ListProperty<RegularFile>

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val staticDependenciesFile: RegularFileProperty

    @get:Input
    abstract val verificationConfig: Property<VerificationConfig>

    @get:OutputDirectory
    abstract val workingDir: DirectoryProperty

    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun analyze() {
        workerExecutor.noIsolation().submit(RulerTaskAction::class.java) {
            it.dependencyEntries.set(dependencyEntries)
            it.projectPath.set(projectPath)
            it.sdkDirectory.set(sdkDirectory)
            it.appInfo.set(appInfo)
            it.deviceSpec.set(deviceSpec)
            it.bundleFile.set(bundleFile)
            it.mappingFile.set(mappingFile)
            it.resourceMappingFile.set(resourceMappingFile)
            it.ownershipFile.set(ownershipFile)
            it.defaultOwner.set(defaultOwner)
            it.omitFileBreakdown.set(omitFileBreakdown)
            it.unstrippedNativeFiles.set(unstrippedNativeFiles)
            it.staticDependenciesFile.set(staticDependenciesFile)
            it.verificationConfig.set(verificationConfig)
            it.workingDir.set(workingDir)
            it.reportDir.set(reportDir)
        }
    }

    abstract class Params : WorkParameters {
        abstract val dependencyEntries: MapProperty<String, List<DependencyEntry>>
        abstract val projectPath: Property<String>
        abstract val sdkDirectory: DirectoryProperty
        abstract val appInfo: Property<AppInfo>
        abstract val deviceSpec: Property<DeviceSpec>
        abstract val bundleFile: RegularFileProperty
        abstract val mappingFile: RegularFileProperty
        abstract val resourceMappingFile: RegularFileProperty
        abstract val ownershipFile: RegularFileProperty
        abstract val defaultOwner: Property<String>
        abstract val omitFileBreakdown: Property<Boolean>
        abstract val unstrippedNativeFiles: ListProperty<RegularFile>
        abstract val staticDependenciesFile: RegularFileProperty
        abstract val verificationConfig: Property<VerificationConfig>
        abstract val workingDir: DirectoryProperty
        abstract val reportDir: DirectoryProperty
    }

    abstract class RulerTaskAction : WorkAction<Params>, BaseRulerTask {

        override fun execute() {
            run()
        }

        private val config by lazy {
            RulerConfig(
                projectPath = parameters.projectPath.get(),
                apkFilesMap = createApkFile(),
                reportDir = parameters.reportDir.asFile.get(),
                ownershipFile = parameters.ownershipFile.asFile.orNull,
                staticDependenciesFile = parameters.staticDependenciesFile.asFile.orNull,
                appInfo = parameters.appInfo.get(),
                deviceSpec = parameters.deviceSpec.get(),
                defaultOwner = parameters.defaultOwner.get(),
                omitFileBreakdown = parameters.omitFileBreakdown.get(),
                additionalEntries = emptyList(),
                ignoredFiles = emptyList(),
                verificationConfig = parameters.verificationConfig.get(),
            )
        }

        override fun rulerConfig(): RulerConfig = config

        override fun provideDependencies(): Map<String, List<DependencyComponent>> {
            val classNameSanitizer = ClassNameSanitizer(provideMappingFile())
            val dependencySanitizer = DependencySanitizer(classNameSanitizer)
            return dependencySanitizer.sanitize(parameters.dependencyEntries.get().values.flatten())
        }

        override fun print(content: String) = println(content) // Use println directly?
        override fun provideMappingFile(): File? = parameters.mappingFile.asFile.orNull
        override fun provideResourceMappingFile(): File? = parameters.resourceMappingFile.asFile.orNull
        override fun provideUnstrippedLibraryFiles(): List<File> = parameters.unstrippedNativeFiles.get().map {
            it.asFile
        }

        override fun provideBloatyPath() = null

        private fun createApkFile(): Map<String, List<File>> {
            val apkCreator = ApkCreator(parameters.sdkDirectory.asFile.get())

            val apkFile = parameters.bundleFile.asFile.get()
            return if (apkFile.extension == "apk") {
                mapOf(ApkCreator.BASE_FEATURE_NAME to listOf(apkFile))
            } else {
                apkCreator.createSplitApks(
                    apkFile,
                    parameters.deviceSpec.get(),
                    parameters.workingDir.asFile.get(),
                )
            }
        }
    }
}
