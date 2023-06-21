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

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.guardsquare.proguard")
    id("io.gitlab.arturbosch.detekt")
    id("com.spotify.ruler")
}

android {
    namespace = "com.spotify.ruler.sample.proguard"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.spotify.ruler.sample"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    lint {
        warningsAsErrors = true
    }
    packagingOptions {
        resources.excludes.add("**/*.kotlin_builtins")
        resources.excludes.add("kotlin-tooling-metadata.json")
    }

    // Workaround until https://github.com/Guardsquare/proguard/issues/272 is fixed
    androidResources {
        additionalParameters("--proguard", project.buildDir.resolve("intermediates/proguard/configs/aapt_rules.pro").absolutePath)
    }
    applicationVariants.configureEach {
        val variantName = StringGroovyMethods.capitalize(name)
        tasks.named("bundle${variantName}Resources").configure { dependsOn("process${variantName}Resources") }
    }
}

dependencies {
    implementation(project(":sample:lib"))
}

proguard {
    configurations {
        register("release") {
            defaultConfiguration("proguard-android-optimize.txt")
        }
    }
}

ruler {
    abi.set("arm64-v8a")
    locale.set("en")
    screenDensity.set(480)
    sdkVersion.set(27)
}

// Include Ruler tasks in checks
tasks.named("check").configure {
    dependsOn("analyzeDebugBundle")
    dependsOn("analyzeReleaseBundle")
}
