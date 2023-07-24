package com.spotify.ruler.common.models

import java.io.File

data class RulerCompareConfig(
    val projectPath: String,
    val headAab: AabConfig,
    val baseAab: AabConfig,
    val deviceSpec: DeviceSpec,
    val reportDir: File,
)
