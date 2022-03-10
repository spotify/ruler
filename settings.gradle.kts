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

rootProject.name = "ruler"

include(":ruler-frontend")
include(":ruler-frontend-tests")
include(":ruler-gradle-plugin")
include(":ruler-models")

if (!startParameter.projectProperties.containsKey("withoutSample")) {
    include(":ruler-e2e-tests")

    include(":sample:app")
    include(":sample:lib")
}

plugins {
    id("com.gradle.enterprise") version "3.8.1" // https://mvnrepository.com/artifact/com.gradle.enterprise/com.gradle.enterprise.gradle.plugin
}

gradleEnterprise {
    val isCiBuild = System.getenv("CI").toBoolean()

    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        if (isCiBuild) {
            termsOfServiceAgree = "yes"
            publishAlways()
        }
    }
}
