/*
* Copyright 2021 Spotify AB
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

package com.spotify.ruler.models

/** Piece of an app whose size can be measured. */
interface Measurable {
    val downloadSize: Long
    val installSize: Long

    /**
     * The size of an app can be measured in one of two ways:
     * 1. Download size - the size of a file that has to be downloaded.
     * 2. Install size - the size of a file once it's installed on a device.
     */
    enum class SizeType { DOWNLOAD, INSTALL }

    /** Retrieves the size in bytes, based on the given [sizeType]. */
    fun getSize(sizeType: SizeType) = when(sizeType) {
        SizeType.DOWNLOAD -> downloadSize
        SizeType.INSTALL -> installSize
    }

    /** A mutable [Measurable] implementation. */
    data class Mutable(override var downloadSize: Long, override var installSize: Long) : Measurable
}
