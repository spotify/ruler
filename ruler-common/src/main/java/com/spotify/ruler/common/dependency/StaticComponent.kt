package com.spotify.ruler.common.dependency

import kotlinx.serialization.Serializable

@Serializable
data class StaticComponent(
    val path: String,
    val id: String
)
