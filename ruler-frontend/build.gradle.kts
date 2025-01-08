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

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig (Action {
                cssSupport {
                    enabled.set(true)
                }
            })
        }
        binaries.executable()
    }
}

dependencies {
    "jsMainImplementation"(project(":ruler-models"))

    "jsMainImplementation"(Dependencies.KOTLIN_REACT)
    "jsMainImplementation"(Dependencies.KOTLIN_REACT_DOM)
    "jsMainImplementation"(Dependencies.KOTLIN_REACT_ROUTER)
    "jsMainImplementation"(Dependencies.KOTLIN_JS_EXTENSIONS)
    "jsMainImplementation"(Dependencies.KOTLINX_SERIALIZATION_JSON)

    "jsMainImplementation"(npm(Dependencies.REACT, Dependencies.Versions.REACT))
    "jsMainImplementation"(npm(Dependencies.REACT_DOM, Dependencies.Versions.REACT))
    "jsMainImplementation"(npm(Dependencies.BOOTSTRAP, Dependencies.Versions.BOOTSTRAP))
    "jsMainImplementation"(npm(Dependencies.APEX_CHARTS, Dependencies.Versions.APEX_CHARTS))

    "jsTestImplementation"(kotlin("test-js"))
}

val browserDist: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(browserDist.name, tasks.named("jsBrowserDistribution").map { it.outputs.files.files.single() })
}

tasks.named("jsBrowserDevelopmentRun") {
    dependsOn("jsDevelopmentExecutableCompileSync")
}

tasks.named("jsBrowserDevelopmentWebpack") {
    dependsOn("jsProductionExecutableCompileSync")
}

tasks.named("jsBrowserProductionWebpack") {
    dependsOn("jsDevelopmentExecutableCompileSync")
}
