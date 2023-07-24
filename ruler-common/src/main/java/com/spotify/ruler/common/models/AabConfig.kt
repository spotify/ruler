package com.spotify.ruler.common.models

import java.io.File

data class AabConfig(
    val aab: File,
    val proguardMap: File,
    val resourceMap: File
)
