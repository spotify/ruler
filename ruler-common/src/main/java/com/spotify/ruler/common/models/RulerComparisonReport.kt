package com.spotify.ruler.common.models

import kotlinx.serialization.Serializable

@Serializable
data class RulerComparisonReport(
    val newAppDownloadSize: Long,
    val newAppInstallSize: Long,
    val oldAppDownloadSize: Long,
    val oldAppInstallSize: Long,
    val totalSizeDifference: Long,
    val filesChanged: List<DifferentAppFile>
)
