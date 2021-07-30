plugins {
    id("org.jetbrains.kotlin.js")
    id("com.bnorm.react.kotlin-react-function")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
        binaries.executable()
    }
}

dependencies {
    implementation(project(":ruler-models"))

    implementation(Dependencies.KOTLIN_REACT)
    implementation(Dependencies.KOTLIN_REACT_DOM)
    implementation(Dependencies.KOTLIN_JS_EXTENSIONS)
    implementation(Dependencies.KOTLIN_REACT_FUNCTION)
    implementation(Dependencies.KOTLINX_SERIALIZATION_JSON)

    implementation(npm(Dependencies.REACT, Dependencies.Versions.REACT))
    implementation(npm(Dependencies.REACT_DOM, Dependencies.Versions.REACT))
    implementation(npm(Dependencies.BOOTSTRAP, Dependencies.Versions.BOOTSTRAP))

    testImplementation(kotlin("test-js"))
}
