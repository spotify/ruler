package com.spotify.ruler.common.compare

import com.spotify.ruler.common.models.DifferentAppFile
import com.spotify.ruler.common.models.RulerComparisonReport
import com.spotify.ruler.models.AppFile

class RulerComparer {

    fun compareBuilds(head: List<AppFile>, base: List<AppFile>): RulerComparisonReport {
        val newDownloadSize = head.sumOf { it.downloadSize }
        val newInstallSize = head.sumOf { it.installSize }
        val oldDownloadSize = base.sumOf { it.downloadSize }
        val oldInstallSize = base.sumOf { it.installSize }
        val totalDifference = oldDownloadSize - newDownloadSize
        val differenceList = findDifference(head, base).sortedByDescending { it.difference }
        return RulerComparisonReport(
            newDownloadSize,
            newInstallSize,
            oldDownloadSize,
            oldInstallSize,
            totalDifference,
            differenceList
        )
    }

    fun findDifference(head: List<AppFile>, base: List<AppFile>): List<DifferentAppFile> {
        val differenceList = mutableListOf<DifferentAppFile>()

        val baseFileMap = base.associateBy { it.name }

        for (headFile in head) {
            val baseFile = baseFileMap[headFile.name]
            if (baseFile != null) {
                if (headFile.downloadSize != baseFile.downloadSize) {
                    val difference = headFile.downloadSize - baseFile.downloadSize
                    val differentAppFile = DifferentAppFile(
                        headFile.name,
                        baseFile.downloadSize,
                        headFile.downloadSize,
                        difference,
                    )
                    differenceList.add(differentAppFile)
                }
            } else {
                // Handle the case where the file exists in head but not in base
                val differentAppFile = DifferentAppFile(
                    headFile.name,
                    0,
                    headFile.downloadSize,
                    headFile.downloadSize,
                )
                differenceList.add(differentAppFile)
            }
        }

        return differenceList
    }
}
