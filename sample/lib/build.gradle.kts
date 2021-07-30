plugins {
    id("com.android.library")
}

android {
    compileSdkVersion(31)
    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(31)
    }
    lintOptions {
        isWarningsAsErrors = true
    }
}
