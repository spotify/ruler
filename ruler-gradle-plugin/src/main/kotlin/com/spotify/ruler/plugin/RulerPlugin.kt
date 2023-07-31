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

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.models.DeviceSpec
import org.codehaus.groovy.runtime.StringGroovyMethods
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

class RulerPlugin : Plugin<Project> {

    private val name: String = "ruler"

    @Suppress("UnstableApiUsage")
    override fun apply(project: Project) {
        val rulerExtension = project.extensions.create(name, RulerExtension::class.java)

        project.plugins.withId("com.android.application") {
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->
                val variantName = StringGroovyMethods.capitalize(variant.name)
                project.tasks.register(
                    "analyze${variantName}Bundle",
                    RulerTask::class.java
                ) { task ->
                    task.group = name
                    task.appInfo.set(getAppInfo(project, variant))
                    task.deviceSpec.set(getDeviceSpec(rulerExtension))

                    task.bundleFile.set(getBundleFile(project, variant))
                    task.mappingFile.set(getMappingFile(project, variant))
                    task.resourceMappingFile.set(getResourceMappingFile(project, variant))
                    task.ownershipFile.set(rulerExtension.ownershipFile)
                    task.defaultOwner.set(rulerExtension.defaultOwner)

                    task.workingDir.set(project.layout.buildDirectory.dir("intermediates/ruler/${variant.name}"))
                    task.reportDir.set(project.layout.buildDirectory.dir("reports/ruler/${variant.name}"))

                    task.omitFileBreakdown.set(rulerExtension.omitFileBreakdown)
                    task.unstrippedNativeFiles.set(rulerExtension.unstrippedNativeFiles)

                    // Add explicit dependency to support DexGuard
                    task.dependsOn("bundle$variantName")
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun getAppInfo(project: Project, variant: ApplicationVariant) = project.provider {
        AppInfo(
            applicationId = variant.applicationId.get(),
            versionName = variant.outputs.first().versionName.get() ?: "-",
            variantName = variant.name,
        )
    }

    private fun getDeviceSpec(extension: RulerExtension) = DeviceSpec(
        abi = extension.abi.orNull ?: error("ABI not specified."),
        locale = extension.locale.orNull ?: error("Locale not specified."),
        screenDensity = extension.screenDensity.orNull ?: error("Screen density not specified."),
        sdkVersion = extension.sdkVersion.orNull ?: error("SDK version not specified."),
    )

    /**
     * Returns the bundle file that's going to be analyzed. DexGuard produces a separate bundle instead of overriding
     * the default one, so we have to handle that separately.
     */
    private fun getBundleFile(
        project: Project,
        variant: ApplicationVariant
    ): Provider<RegularFile> {
        val defaultBundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)
        if (!hasDexGuard(project)) {
            return defaultBundleFile // No DexGuard means we can use the default bundle
        }

        // Bundle can still be in the default location, depending on the DexGuard config
        return defaultBundleFile.flatMap { bundle ->
            val dexGuardBundle =
                bundle.asFile.parentFile.resolve("${bundle.asFile.nameWithoutExtension}-protected.aab")
            if (dexGuardBundle.exists()) {
                project.layout.buildDirectory.file(dexGuardBundle.absolutePath) // File exists -> use it
            } else {
                defaultBundleFile // File doesn't exist -> fall back to default
            }
        }
    }

    /**
     * Returns the mapping file used for de-obfuscation. Different obfuscation tools like DexGuard and ProGuard place
     * their mapping files in different directories, so we have to handle those separately.
     */
    private fun getMappingFile(
        project: Project,
        variant: ApplicationVariant
    ): Provider<RegularFile> {
        val defaultMappingFile = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
        val mappingFilePath = when {
            hasDexGuard(project) -> "outputs/dexguard/mapping/bundle/${variant.name}/mapping.txt"
            hasProGuard(project) -> "outputs/proguard/${variant.name}/mapping/mapping.txt"
            else -> return defaultMappingFile // No special obfuscation plugin -> use default path
        }

        // Mapping files can also be missing, for example when obfuscation is disabled for a variant
        val mappingFileProvider = project.layout.buildDirectory.file(mappingFilePath)
        return mappingFileProvider.flatMap { mappingFile ->
            if (mappingFile.asFile.exists()) {
                mappingFileProvider // File exists -> use it
            } else {
                defaultMappingFile // File doesn't exist -> fall back to default
            }
        }
    }

    /**
     * Returns a mapping file to de-obfuscate resource names. DexGuard supports this feature by default, so we need to
     * handle it accordingly.
     */
    private fun getResourceMappingFile(
        project: Project,
        variant: ApplicationVariant
    ): Provider<RegularFile> {
        val defaultResourceMappingFile = project.objects.fileProperty() // Empty by default
        val resourceMappingFilePath = when {
            hasDexGuard(project) -> "outputs/dexguard/mapping/bundle/${variant.name}/resourcefilenamemapping.txt"
            else -> return defaultResourceMappingFile // No DexGuard plugin -> use default empty file
        }

        // Mapping file can still be missing, for example if resource obfuscation is disabled for a variant
        val resourceMappingFileProvider =
            project.layout.buildDirectory.file(resourceMappingFilePath)
        return resourceMappingFileProvider.flatMap { resourceMappingFile ->
            if (resourceMappingFile.asFile.exists()) {
                resourceMappingFileProvider // File exists -> use it
            } else {
                defaultResourceMappingFile // File doesn't exist -> fall back to default
            }
        }
    }

    /** Checks if the given [project] is using DexGuard for obfuscation, instead of R8. */
    private fun hasDexGuard(project: Project): Boolean {
        return project.pluginManager.hasPlugin("dexguard")
    }

    /** Checks if the given [project] is using ProGuard for obfuscation, instead of R8. */
    private fun hasProGuard(project: Project): Boolean {
        return project.pluginManager.hasPlugin("com.guardsquare.proguard")
    }
}
