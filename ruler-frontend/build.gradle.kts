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
    id("org.jetbrains.kotlin.js")
    id("io.gitlab.arturbosch.detekt")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(11)

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }
}

dependencies {
    implementation(project(":ruler-models"))

    implementation(Dependencies.KOTLIN_REACT)
    implementation(Dependencies.KOTLIN_REACT_DOM)
    implementation(Dependencies.KOTLIN_REACT_ROUTER)
    implementation(Dependencies.KOTLIN_JS_EXTENSIONS)
    implementation(Dependencies.KOTLINX_SERIALIZATION_JSON)

    implementation(npm(Dependencies.REACT, Dependencies.Versions.REACT))
    implementation(npm(Dependencies.REACT_DOM, Dependencies.Versions.REACT))
    implementation(npm(Dependencies.BOOTSTRAP, Dependencies.Versions.BOOTSTRAP))
    implementation(npm(Dependencies.APEX_CHARTS, Dependencies.Versions.APEX_CHARTS))

    testImplementation(kotlin("test-js"))
}

val browserDist by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(browserDist.name, tasks.named("browserDistribution").map { it.outputs.files.files.single() })
}

// FIXME Reason: Task ':ruler-frontend:browserDevelopmentWebpack' uses this output of
//  task ':ruler-frontend:productionExecutableCompileSync' without declaring an explicit or implicit dependency.
tasks.named("productionExecutableCompileSync") {
    mustRunAfter("browserDevelopmentWebpack")
}
