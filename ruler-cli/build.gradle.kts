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
    id("application")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
    id("com.github.johnrengelman.shadow")
    id("maven-publish")
    id("signing")
}

extra[EXT_POM_NAME] = "Ruler CLI"
extra[EXT_POM_DESCRIPTION] = "Command line interface for Ruler"

java {
    withSourcesJar()

}

kotlin {
    jvmToolchain(17)
}


dependencies {
    implementation(project(":ruler-models"))
    implementation(project(":ruler-common"))
    implementation(Dependencies.CLIKT)
    implementation(Dependencies.KOTLINX_SERIALIZATION_JSON)

    testRuntimeOnly(Dependencies.JUNIT_ENGINE)
    testImplementation(Dependencies.JUNIT_API)
    testImplementation(Dependencies.JUNIT_PARAMS)
    testImplementation(Dependencies.GOOGLE_TRUTH)
}

application {
    mainClass.set("com.spotify.ruler.cli.RulerCliKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks["shadowJar"])
        }
    }
    configurePublications(project)
}

signing {
    configureSigning(publishing.publications)
}
