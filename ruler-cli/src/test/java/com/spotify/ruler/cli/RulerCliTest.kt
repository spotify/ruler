package com.spotify.ruler.cli

import com.google.common.truth.Truth
import com.spotify.ruler.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class RulerCliTest {

    private val dependencyMap = Paths.get("src", "test", "resources", "dependencyMap.json").toFile()
    private val rulerConfig = Paths.get("src", "test", "resources", "rulerConfig.json").toFile()
    private val testApk = Paths.get("src", "test", "resources", "test.apk").toFile()

    private val proguardFile = Paths.get("src", "test", "resources", "test_proguard.map").toFile()

    private val jsonResult = Paths.get("src", "test", "resources", "report.json").toFile()
    private val htmlResult = Paths.get("src", "test", "resources", "report.html").toFile()

    @Test
    fun `Test all required cli arguments are passing`() {
        RulerCli().parse(
            listOf(
                "--dependency-map", dependencyMap.path,
                "--ruler-config-json", rulerConfig.path,
                "--apk-file", testApk.path,
                "--report-dir", "src/test/resources/"
            )
        )
        val appReport = Json.decodeFromStream<AppReport>(jsonResult.inputStream())
        Truth.assertThat(appReport.name).isEqualTo("com.ruler.example-bazel")
        Truth.assertThat(appReport.version).isEqualTo("1.0.0")
        Truth.assertThat(appReport.downloadSize).isEqualTo(14013)
        Truth.assertThat(appReport.installSize).isEqualTo(14591)
        Truth.assertThat(appReport.components.size).isEqualTo(1)

        val appComponent = appReport.components[0]
        Truth.assertThat(appComponent.name).isEqualTo("RulerTest")
        Truth.assertThat(appComponent.type.toString()).isEqualTo("INTERNAL")
        Truth.assertThat(appComponent.downloadSize).isEqualTo(14013)
        Truth.assertThat(appComponent.installSize).isEqualTo(14591)
        Truth.assertThat(appComponent.files?.size).isEqualTo(16)

        // Test ownership file properly attributes owners
        val attributedList = appComponent.files?.filter { it.owner == "ruler-test-team" }
        Truth.assertThat(attributedList).containsExactly(
            AppFile(
                "com.spotify.ruler.sample.MainActivity",
                type = FileType.CLASS,
                downloadSize = 468,
                installSize = 468,
                owner = "ruler-test-team",
            ),
            AppFile(
                "/res/layout/activity_main.xml",
                type = FileType.RESOURCE,
                downloadSize = 257,
                installSize = 257,
                owner = "ruler-test-team",
                resourceType = ResourceType.LAYOUT
            )
        )
    }

    @Test
    fun `Test optional cli arguments are passing`() {
        val cli = RulerCli()
        cli.parse(
            listOf(
                "--dependency-map", dependencyMap.path,
                "--ruler-config-json", rulerConfig.path,
                "--apk-file", testApk.path,
                "--report-dir", "src/test/resources/",
                "--mapping-file", proguardFile.path
            )
        )
        Truth.assertThat(cli.provideMappingFile()).isNotNull()
        Truth.assertThat(cli.provideMappingFile()?.path).isEqualTo(proguardFile.path)
    }

    // Clean reports after each test
    @AfterEach
    fun cleanReports() {
        jsonResult.delete()
        htmlResult.delete()
    }
}
