package com.spotify.ruler.common.models

import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.ResourceType
import kotlinx.serialization.Serializable

@Serializable
data class DifferentAppFile(
    val name: String,
    val oldSize: Long,
    val newSize: Long,
    val difference: Long,
)
