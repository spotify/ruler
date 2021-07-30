plugins {
    id("com.android.application")
    id("com.spotify.ruler")
}

android {
    compileSdkVersion(31)
    defaultConfig {
        applicationId = "com.spotify.ruler.sample"
        minSdkVersion(23)
        targetSdkVersion(31)
        versionCode(1)
        versionName("1.0")
    }
    buildTypes {
        named("release").configure {
            minifyEnabled(true)
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    lintOptions {
        isWarningsAsErrors = true
    }
}

dependencies {
    implementation(project(":sample:lib"))
}

ruler {
    abi.set("arm64-v8a")
    locale.set("en")
    screenDensity.set(480)
    sdkVersion.set(27)
}
