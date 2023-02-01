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

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
    id("maven-publish")
    id("signing")
}

extra[EXT_POM_NAME] = "Ruler common"
extra[EXT_POM_DESCRIPTION] = "Common code used by the Ruler Gradle plugin & Ruler CLI"

java {
    withSourcesJar()

    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

// Required for copying resources from Kotln JS (ruler-frontend) module.
val browserDist by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(Dependencies.ANDROID_GRADLE_PLUGIN)
    compileOnly(Dependencies.BUNDLETOOL)
    compileOnly(Dependencies.PROTOBUF_CORE)
    compileOnly(Dependencies.ANDROID_TOOLS_COMMON)
    compileOnly(Dependencies.ANDROID_TOOLS_SDKLIB)
    compileOnly(Dependencies.DEXLIB)

    implementation(project(":ruler-models"))


    implementation(Dependencies.APK_ANALYZER) {
        exclude(group = "com.android.tools.lint") // Avoid leaking incompatible Lint versions to consumers
    }
    implementation(Dependencies.KOTLINX_SERIALIZATION_JSON)
    implementation(Dependencies.SNAKE_YAML)

    testRuntimeOnly(Dependencies.JUNIT_ENGINE)
    testImplementation(gradleTestKit())
    testImplementation(Dependencies.JUNIT_API)
    testImplementation(Dependencies.JUNIT_PARAMS)
    testImplementation(Dependencies.GOOGLE_TRUTH)
    testImplementation(Dependencies.GOOGLE_GUAVA)

    browserDist(
        project(
            mapOf(
                "path" to ":ruler-frontend",
                "configuration" to "browserDist"
            )
        )
    )
}

tasks.withType<Copy>().named("processResources") {
    from(browserDist)
}

publishing {
    publications {
        create<MavenPublication>("jvm") {
            from(components["java"])
        }
    }
    configurePublications(project)
}


tasks.withType<Test> {
    useJUnitPlatform()
}

signing {
    configureSigning(publishing.publications)
}
