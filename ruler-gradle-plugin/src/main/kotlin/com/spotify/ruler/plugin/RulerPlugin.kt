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

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.models.DeviceSpec
import com.spotify.ruler.common.veritication.VerificationConfig
import com.spotify.ruler.plugin.dependency.EntryParser
import org.codehaus.groovy.runtime.StringGroovyMethods
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

class RulerPlugin : Plugin<Project> {

    private val name: String = "ruler"

    override fun apply(project: Project) {
        val rulerExtension = project.extensions.create(name, RulerExtension::class.java)
        val rulerVerificationExtension = (rulerExtension as ExtensionAware).extensions.create(
            "verification",
            RulerVerificationExtension::class.java
        )

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

                    val resolve = EntryParser().parse(variant.runtimeConfiguration)

                    resolve.forEach(task.dependencyEntries::put)

                    task.projectPath.set(project.path)
                    task.sdkDirectory.set(androidComponents.sdkComponents.sdkDirectory)

                    task.appInfo.set(getAppInfo(project, variant))
                    task.deviceSpec.set(getDeviceSpec(rulerExtension))

                    task.bundleFile.set(project.getBundleFile(variant))
                    task.mappingFile.set(project.getMappingFile(variant))
                    task.resourceMappingFile.set(project.getResourceMappingFile(variant))
                    task.ownershipFile.set(rulerExtension.ownershipFile)
                    task.defaultOwner.set(rulerExtension.defaultOwner)

                    task.workingDir.set(project.layout.buildDirectory.dir("intermediates/ruler/${variant.name}"))
                    task.reportDir.set(project.layout.buildDirectory.dir("reports/ruler/${variant.name}"))

                    task.staticDependenciesFile.set(rulerExtension.staticDependenciesFile)
                    task.omitFileBreakdown.set(rulerExtension.omitFileBreakdown)
                    task.unstrippedNativeFiles.set(rulerExtension.unstrippedNativeFiles)

                    task.verificationConfig.set(getVerificationConfig(rulerVerificationExtension))

                    // Add explicit dependency to support DexGuard
                    task.dependsOn("bundle$variantName")
                }
            }
        }
    }

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

    private fun getVerificationConfig(extension: RulerVerificationExtension): VerificationConfig {
        return VerificationConfig(
            downloadSizeThreshold = extension.downloadSizeThreshold.get(),
            installSizeThreshold = extension.installSizeThreshold.get()
        )
    }

}
