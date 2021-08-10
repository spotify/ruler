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

import org.codehaus.groovy.runtime.StringGroovyMethods
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

/**
 * Makes a directory available to webpack, so its contents can be loaded and used in the frontend. This is done by
 * copying all files contained in the directory to a folder accessible by webpack, before the webpack task runs.
 *
 * @param sourceDirectory The contents of this directory will be made available to webpack.
 * @param webpackMode Webpack configuration, for which the directory is registered (development or production).
 */
fun Project.registerWebpackDirectory(sourceDirectory: String, webpackMode: String) {
    val mode = StringGroovyMethods.capitalize(webpackMode)
    val targetDirectory = "js/packages/${rootProject.name}-${project.name}/kotlin"

    val task = tasks.register<Copy>("prepare${mode}WebpackData") {
        from(layout.projectDirectory.dir(sourceDirectory))
        into(rootProject.layout.buildDirectory.dir(targetDirectory))
    }

    // Make sure data is copied before webpack runs
    tasks.named("browser${mode}Webpack").configure { dependsOn(task) }
    tasks.named("browser${mode}Run").configure { dependsOn(task) }
}
