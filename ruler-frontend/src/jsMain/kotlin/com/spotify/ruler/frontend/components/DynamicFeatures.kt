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

import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.Measurable
import react.FC
import react.Props
import react.dom.html.ReactHTML.h4
import web.cssom.ClassName


val DynamicFeatures = FC<DynamicFeaturesProps> { props ->
    h4 {
        className = ClassName("mb-3")
        +"Dynamic features" }
    Row {
        ContainerList {
            containers = props.features
            sizeType = props.sizeType
        }
    }
}

external interface DynamicFeaturesProps: Props {
    var features: List<DynamicFeature>
    var sizeType: Measurable.SizeType
}
