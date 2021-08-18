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

object Dependencies {
    const val RULER_GRADLE_PLUGIN = "$RULER_PLUGIN_GROUP:ruler-gradle-plugin:$RULER_PLUGIN_VERSION"

    const val ANDROID_GRADLE_PLUGIN = "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_PLUGIN}"
    const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val KOTLINX_SERIALIZATION_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-serialization:${Versions.KOTLIN}"
    const val KOTLIN_REACT_FUNCTION_GRADLE_PLUGIN = "gradle.plugin.com.bnorm.react:kotlin-react-function-gradle:${Versions.KOTLIN_REACT_FUNCTION}"
    const val DETEKT_GRADLE_PLUGIN = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${Versions.DETEKT_GRADLE_PLUGIN}"
    const val NEXUS_PUBLISH_GRADLE_PLUGIN = "io.github.gradle-nexus.publish-plugin:io.github.gradle-nexus.publish-plugin.gradle.plugin:${Versions.NEXUS_PUBLISH_GRADLE_PLUGIN}"

    const val BUNDLETOOL = "com.android.tools.build:bundletool:${Versions.BUNDLETOOL}"
    const val PROTOBUF_CORE = "com.google.protobuf:protobuf-java:${Versions.PROTOBUF}"
    const val DEXLIB = "org.smali:dexlib2:${Versions.DEXLIB}"
    const val ANDROID_TOOLS_COMMON = "com.android.tools:common:${Versions.ANDROID_TOOLS}"
    const val ANDROID_TOOLS_SDKLIB = "com.android.tools:sdklib:${Versions.ANDROID_TOOLS}"

    const val APK_ANALYZER = "com.android.tools.apkparser:apkanalyzer:${Versions.ANDROID_TOOLS}"
    const val KOTLINX_SERIALIZATION_CORE = "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.KOTLINX_SERIALIZATION}"
    const val KOTLINX_SERIALIZATION_JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}"

    const val JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}"
    const val JUNIT_API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}"
    const val JUNIT_PARAMS = "org.junit.jupiter:junit-jupiter-params:${Versions.JUNIT}"
    const val GOOGLE_TRUTH = "com.google.truth:truth:${Versions.GOOGLE_TRUTH}"
    const val GOOGLE_GUAVA = "com.google.guava:guava:${Versions.GOOGLE_GUAVA}"

    const val KOTLIN_REACT = "org.jetbrains.kotlin-wrappers:kotlin-react:${Versions.KOTLIN_REACT}"
    const val KOTLIN_REACT_DOM = "org.jetbrains.kotlin-wrappers:kotlin-react-dom:${Versions.KOTLIN_REACT}"
    const val KOTLIN_JS_EXTENSIONS = "org.jetbrains.kotlin-wrappers:kotlin-extensions:${Versions.KOTLIN_JS_EXTENSIONS}"
    const val KOTLIN_REACT_FUNCTION = "com.bnorm.react:kotlin-react-function:${Versions.KOTLIN_REACT_FUNCTION}"

    const val REACT = "react"
    const val REACT_DOM = "react-dom"
    const val BOOTSTRAP = "bootstrap"

    object Versions {
        const val ANDROID_GRADLE_PLUGIN = "7.0.1" // https://mvnrepository.com/artifact/com.android.tools.build/gradle?repo=google
        const val KOTLIN = "1.5.21" // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
        const val KOTLIN_REACT_FUNCTION = "0.5.1" // https://mvnrepository.com/artifact/com.bnorm.react.kotlin-react-function/com.bnorm.react.kotlin-react-function.gradle.plugin
        const val DETEKT_GRADLE_PLUGIN = "1.18.0" // https://mvnrepository.com/artifact/io.gitlab.arturbosch.detekt/detekt-gradle-plugin
        const val NEXUS_PUBLISH_GRADLE_PLUGIN = "1.1.0" // https://mvnrepository.com/artifact/io.github.gradle-nexus.publish-plugin/io.github.gradle-nexus.publish-plugin.gradle.plugin

        const val BUNDLETOOL = "1.8.0" // https://mvnrepository.com/artifact/com.android.tools.build/bundletool
        const val PROTOBUF = "3.17.3" // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
        const val DEXLIB = "2.5.2" // https://mvnrepository.com/artifact/org.smali/dexlib2

        const val ANDROID_TOOLS = "30.0.1" // https://mvnrepository.com/artifact/com.android.tools/common?repo=google
        const val KOTLINX_SERIALIZATION = "1.2.2" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-core

        const val JUNIT = "5.7.2" // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine
        const val GOOGLE_TRUTH = "1.1.3" // https://mvnrepository.com/artifact/com.google.truth/truth
        const val GOOGLE_GUAVA = "30.1.1-jre" // https://mvnrepository.com/artifact/com.google.guava/guava

        const val KOTLIN_REACT = "17.0.2-pre.227-kotlin-1.5.21" // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react
        const val KOTLIN_JS_EXTENSIONS = "1.0.1-pre.227-kotlin-1.5.21" // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-extensions

        const val REACT = "17.0.2" // https://www.npmjs.com/package/react
        const val BOOTSTRAP = "5.1.0" // https://www.npmjs.com/package/bootstrap
    }
}
