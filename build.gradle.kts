import io.github.gradlenexus.publishplugin.NexusPublishExtension

buildscript {
    repositories {
        mavenLocal {
            content {
                includeGroup(RULER_PLUGIN_GROUP) // Only load Ruler plugin from local Maven, for the sample project
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(Dependencies.ANDROID_GRADLE_PLUGIN)
        classpath(Dependencies.KOTLIN_GRADLE_PLUGIN)
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
