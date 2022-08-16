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

import io.github.gradlenexus.publishplugin.NexusPublishExtension

buildscript {
    repositories {
        mavenLocal {
            content {
                // Only load Ruler plugin from local Maven (required for the sample project)
                includeGroup(RULER_PLUGIN_GROUP)
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(Dependencies.ANDROID_GRADLE_PLUGIN)
        classpath(Dependencies.KOTLIN_GRADLE_PLUGIN)
        classpath(Dependencies.PROGUARD_GRADLE_PLUGIN)
        classpath(Dependencies.KOTLINX_SERIALIZATION_GRADLE_PLUGIN)
        classpath(Dependencies.KOTLIN_REACT_FUNCTION_GRADLE_PLUGIN)
        classpath(Dependencies.DETEKT_GRADLE_PLUGIN)
        classpath(Dependencies.NEXUS_PUBLISH_GRADLE_PLUGIN)

        if (!properties.containsKey("withoutSample")) {
            classpath(Dependencies.RULER_GRADLE_PLUGIN)
        }
    }
}

apply(plugin = "io.github.gradle-nexus.publish-plugin")

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

group = RULER_PLUGIN_GROUP
version = RULER_PLUGIN_VERSION

extensions.configure(NexusPublishExtension::class) {
    repositories {
        sonatype {
            username.set(System.getenv(ENV_SONATYPE_USERNAME))
            password.set(System.getenv(ENV_SONATYPE_PASSWORD))
        }
    }
}
