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

package com.spotify.ruler.frontend

import com.spotify.ruler.models.Measurable

const val BYTE_FACTOR = 1024
const val PERCENT_FACTOR = 100

fun formatSize(measurable: Measurable, sizeType: Measurable.SizeType): String {
    val bytes = measurable.getSize(sizeType)
    return formatSize(bytes)
}

fun formatSize(bytes: Number): String {
    val units = mutableListOf("B", "KB", "MB", "GB", "TB", "PB")
    var remainder = bytes.toDouble()
    while (remainder > BYTE_FACTOR) {
        remainder /= BYTE_FACTOR
        units.removeFirst()
    }
    return "${remainder.asDynamic().toFixed(1)} ${units.first()}"
}

fun formatPercentage(fraction: Number, total: Number): String {
    val percentage = PERCENT_FACTOR * fraction.toDouble() / total.toDouble()
    return "${percentage.asDynamic().toFixed(2)} %"
}
