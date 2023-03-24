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

package com.spotify.ruler.frontend.components

import androidx.compose.runtime.Composable
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.Measurable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text

@Composable
fun DynamicFeatures(
    features: List<DynamicFeature>,
    sizeType: Measurable.SizeType
) {
    H4(attrs = {
        classes("mb-3")
    }) {
        Text("Dynamic features")
    }
    Div(attrs = {
        classes("row")
    }) {
        ContainerList(
            containers = features,
            sizeType = sizeType
        )
    }
}
