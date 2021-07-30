plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
    id("signing")
    id("io.gitlab.arturbosch.detekt")
}

extra[EXT_POM_NAME] = "Ruler models"
extra[EXT_POM_DESCRIPTION] = "Common models used by the Ruler Gradle plugin"

kotlin {
    jvm()
    js(IR) {
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Dependencies.KOTLINX_SERIALIZATION_CORE)
            }
        }
    }
}

publishing {
    configurePublications(project)
}

signing {
    configureSigning(publishing.publications)
}
