/*
* Copyright 2024 Spotify AB
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.spotify.ruler.common.verificator

import com.spotify.ruler.common.veritication.SizeExceededException
import com.spotify.ruler.common.veritication.VerificationConfig
import com.spotify.ruler.common.veritication.Verificator
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.ResourceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class VerificatorTest {
    private val config = VerificationConfig(100, 100)
    private val verificator = Verificator(config)

    @Test
    fun `Download size under threshold does not trigger SizeExceededException`() {
        val downloadSize = config.downloadSizeThreshold / 2
        val appFiles = generateAppFiles(downloadSize)

        assertDoesNotThrow { verificator.verify(appFiles) }
    }

    @Test
    fun `Download size exceeding threshold triggers SizeExceededException`() {
        val downloadSize = config.downloadSizeThreshold * 2
        val appFiles = generateAppFiles(downloadSize)

        assertThrows<SizeExceededException> { verificator.verify(appFiles) }
    }

    @Test
    fun `Install size under threshold does not trigger SizeExceededException`() {
        val installSize = config.installSizeThreshold / 2
        val appFiles = generateAppFiles(config.downloadSizeThreshold, installSize)

        assertDoesNotThrow { verificator.verify(appFiles) }
    }

    @Test
    fun `Install size exceeding threshold triggers SizeExceededException`() {
        val installSize = config.downloadSizeThreshold * 2
        val appFiles = generateAppFiles(config.downloadSizeThreshold, installSize)

        assertThrows<SizeExceededException> { verificator.verify(appFiles) }
    }

    private fun generateAppFiles(
        downloadSize: Long,
        installSize: Long = downloadSize
    ): List<AppFile> {
        val downloadSizePerFile = downloadSize / 2
        val installSizePerFile = installSize / 2
        return listOf(
            AppFile(
                "com.spotify.MainActivity",
                FileType.CLASS,
                downloadSizePerFile,
                installSizePerFile
            ),
            AppFile(
                "/res/layout/activity_main.xml",
                FileType.RESOURCE,
                downloadSizePerFile,
                installSizePerFile,
                resourceType = ResourceType.LAYOUT
            ),
        )
    }
}
