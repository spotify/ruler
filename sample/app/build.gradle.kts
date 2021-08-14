plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.spotify.ruler")
}

android {
    compileSdk = 31
    defaultConfig {
        applicationId = "com.spotify.ruler.sample"
        minSdk = 23
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    lint {
        isWarningsAsErrors = true
    }
    packagingOptions {
        resources.excludes.add("**/*.kotlin_builtins")
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
