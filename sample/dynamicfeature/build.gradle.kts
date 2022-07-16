plugins {
    id("com.android.dynamic-feature")
    id("org.jetbrains.kotlin.android")
}
android {
    compileSdk = 32

    defaultConfig {
        minSdk = 23
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":sample:app"))
    implementation(project(":sample:lib-dfm"))
    implementation(project(":sample:dynamicfeature2"))
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
}