rootProject.name = "ruler"

include(":ruler-frontend")
include(":ruler-gradle-plugin")
include(":ruler-models")

if (!startParameter.projectProperties.containsKey("withoutSample")) {
    include(":sample:app")
    include(":sample:lib")
}
