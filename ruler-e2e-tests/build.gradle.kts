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
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    testImplementation(project(":ruler-models"))

    testImplementation(Dependencies.KOTLINX_SERIALIZATION_JSON)

    testRuntimeOnly(Dependencies.JUNIT_ENGINE)
    testImplementation(Dependencies.JUNIT_API)
    testImplementation(Dependencies.GOOGLE_TRUTH)
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Make reports of the sample project available
    dependsOn(":sample:app:analyzeDebugBundle")
    dependsOn(":sample:app:analyzeReleaseBundle")
}

kotlin {
    jvmToolchain(17)
}


