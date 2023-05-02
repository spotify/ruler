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
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
    id("signing")
    id("io.gitlab.arturbosch.detekt")
}

extra[EXT_POM_NAME] = "Ruler Gradle plugin"
extra[EXT_POM_DESCRIPTION] = "Gradle plugin for analyzing Android app size"

dependencies {
    compileOnly(gradleApi())
    compileOnly(Dependencies.ANDROID_GRADLE_PLUGIN) {
        val version = System.getenv("ANDROID_GRADLE_PLUGIN_VERSION")
        if (version != null) {
            version {
                strictly(version)
            }
        }
    }
    compileOnly(Dependencies.BUNDLETOOL)
    compileOnly(Dependencies.PROTOBUF_CORE)
    compileOnly(Dependencies.ANDROID_TOOLS_COMMON)
    compileOnly(Dependencies.ANDROID_TOOLS_SDKLIB)
    compileOnly(Dependencies.DEXLIB)

    implementation(project(":ruler-models"))
    implementation(project(":ruler-common"))

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

    // Allow testing against different Android Gradle plugin versions
    testImplementation(Dependencies.ANDROID_GRADLE_PLUGIN) {
        val version = System.getenv("ANDROID_GRADLE_PLUGIN_VERSION")
        if (version != null) {
            version {
                strictly(version)
            }
        }
    }
}

// Include the output of the frontend JS compilation in the plugin resources
sourceSets.main {
    resources.srcDir(project(":ruler-frontend").tasks.named("browserDistribution"))
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Make plugin available to integration tests
    dependsOn("publishToMavenLocal", ":ruler-models:publishToMavenLocal")
}

java {
    withSourcesJar()

    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

publishing {
    publications {
        create<MavenPublication>("jvm") {
            from(components["java"])
        }
    }
    configurePublications(project)
}

signing {
    configureSigning(publishing.publications)
}
