buildscript {
    repositories {
        mavenLocal {
            content {
                includeGroup("com.spotify.ruler") // Only load Ruler plugin from local Maven, for the sample project
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

        if (!properties.containsKey("withoutSample")) {
            classpath(Dependencies.RULER_GRADLE_PLUGIN)
        }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
