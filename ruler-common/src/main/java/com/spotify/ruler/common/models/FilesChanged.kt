package com.spotify.ruler.common.models

import kotlinx.serialization.Serializable

@Serializable
data class FilesChanged(
    val addedFiles: List<DifferentAppFile>,
    val removedFiles: List<DifferentAppFile>,
    val modifiedFiles: List<DifferentAppFile>
)
