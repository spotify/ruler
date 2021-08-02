rootProject.name = "ruler"

include(":ruler-frontend")
include(":ruler-gradle-plugin")
include(":ruler-models")

if (!startParameter.projectProperties.containsKey("withoutSample")) {
    include(":sample:app")
    include(":sample:lib")
}

plugins {
    id("com.gradle.enterprise") version "3.6.3"
}

gradleEnterprise {
    val isCiBuild = System.getenv("CI").toBoolean()

    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        if (isCiBuild) {
            termsOfServiceAgree = "yes"
            publishAlways()
        }
    }
}
