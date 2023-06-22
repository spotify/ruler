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

import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.Measurable
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.h4
import web.cssom.ClassName

val ContainerDynamicFeatureListItemHeader = FC<DynamicFeatureListItemProps> { props ->
    val containsComponenets = props.container.components.isNotEmpty()
    ReactHTML.h2 {
        className = ClassName("accordion-header")
        var classes = "accordion-button collapsed"
        if (!containsComponenets) {
            classes = "$classes disabled"
        }
        ReactHTML.button {
            asDynamic()["data-bs-toggle"] = "expand"
            asDynamic()["data-bs-target"] = "#module-dc-${props.id}-body"

            className = ClassName(classes)

            ReactHTML.span {
                className = ClassName("font-monospace text-truncate me-3")
                +props.container.name
            }
            props.container.owner?.let { owner ->
                ReactHTML.span {
                    className = ClassName("badge bg-secondary me-3")
                    +owner
                }
            }
            var sizeClasses = "ms-auto text-nowrap"
            if (containsComponenets) {
                sizeClasses = "$sizeClasses me-3"
            }
            ReactHTML.span {
                className = ClassName(sizeClasses)
                +formatSize(props.container, props.sizeType)
            }
        }
    }
}

val DynamicContainerListItemBody = FC<ContainerListItemProps> { props ->
    ReactHTML.div {
        className = ClassName("accordion-collapse collapse")
        id = "module-dc-${props.id}-body"
        ReactHTML.div {
            className = ClassName("accordion-body p-0")
            FileList {
                files = props.container.files ?: emptyList()
                sizeType = props.sizeType
            }
        }
    }
}

val DynamicContainerListItem = FC<ContainerListItemProps> { props ->
    ReactHTML.div {
        className = ClassName("accordion-item")
        ContainerListItemHeader {
            id = props.id
            container = props.container
            sizeType = props.sizeType
        }
        DynamicContainerListItemBody {
            id = props.id
            container = props.container
            sizeType = props.sizeType
        }
    }
}

val DynamicContainerList = FC<ContainerListProps> { props ->
    ReactHTML.div {
        className = ClassName("accordion")
        props.containers.forEachIndexed { index, container ->
            DynamicContainerListItem {
                id = index
                ContainerListItem@ this.container = container
                sizeType = props.sizeType
                key = container.name
            }
        }
    }
}


val DynamicModuleList = FC<DynamicFeaturesProps> { props ->
    ReactHTML.div {
        className = ClassName("accordion")
        props.features.forEachIndexed { index, dynamicFeature ->
            Row {
                ContainerDynamicFeatureListItemHeader {
                    id = index
                    container = dynamicFeature
                    sizeType = props.sizeType
                }
                DynamicContainerList {
                    containers = dynamicFeature.components
                    sizeType = props.sizeType
                }
            }
        }
    }
}

val DynamicFeatures = FC<DynamicFeaturesProps> { props ->
    h4 {
        className = ClassName("mb-3")
        +"Dynamic features"
    }
    Row {
        DynamicModuleList {
            features = props.features
            sizeType = props.sizeType
        }
    }
}

external interface DynamicFeaturesProps : Props {
    var features: List<DynamicFeature>
    var sizeType: Measurable.SizeType
}

external interface DynamicFeatureListItemProps : Props {
    var id: Int
    var container: DynamicFeature
    var sizeType: Measurable.SizeType
}
