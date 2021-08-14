plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 23
        targetSdk = 31
    }
    lint {
        isWarningsAsErrors = true
    }
}
