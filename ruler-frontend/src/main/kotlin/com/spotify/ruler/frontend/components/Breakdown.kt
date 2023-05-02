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
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileContainer
import com.spotify.ruler.models.Measurable
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.span
import web.cssom.ClassName

external interface BreakdownProps : Props {
    var components: List<AppComponent>
    var sizeType: Measurable.SizeType
}

val Breakdown = FC<BreakdownProps> { props ->
    h4 {
        className = ClassName("mb-3")
        +"Breakdown (${props.components.size} components)"
    }
    Row {
        ContainerList {
            containers = props.components
            sizeType = props.sizeType
        }
    }
}

external interface ContainerListProps : Props {
    var containers: List<FileContainer>
    var sizeType: Measurable.SizeType
}

val ContainerList = FC<ContainerListProps> { props ->
    div {
        className = ClassName("accordion")
        props.containers.forEachIndexed { index, container ->
            ContainerListItem {
                id = index
                ContainerListItem@this.container = container
                sizeType = props.sizeType
                key = container.name
            }
        }
    }
}

external interface ContainerListItemProps: Props {
   var id: Int
   var container: FileContainer
   var sizeType: Measurable.SizeType
}

val ContainerListItem = FC<ContainerListItemProps> { props ->
    div {
        className = ClassName("accordion-item")
        ContainerListItemHeader {
            id = props.id
            container = props.container
            sizeType = props.sizeType
        }
        ContainerListItemBody {
            id = props.id
            container = props.container
            sizeType = props.sizeType
        }
    }
}

val ContainerListItemHeader = FC<ContainerListItemProps> { props ->
    val containsFiles = props.container.files != null
    h2 {
        className = ClassName("accordion-header")
        var classes = "accordion-button collapsed"
        if (!containsFiles) {
            classes = "$classes disabled"
        }
        button {
            asDynamic()["data-bs-toggle"] = "collapse"
            asDynamic()["data-bs-target"] = "#module-${props.id}-body"

            className = ClassName(classes)

            span {
                className = ClassName( "font-monospace text-truncate me-3")
                +props.container.name
            }
            props.container.owner?.let { owner ->
                span {
                    className = ClassName( "badge bg-secondary me-3")
                    +owner
                }
            }
            var sizeClasses = "ms-auto text-nowrap"
            if (containsFiles) {
                sizeClasses = "$sizeClasses me-3"
            }
            span {
                className = ClassName(sizeClasses)
                +formatSize(props.container, props.sizeType)
            }
        }
    }}

val ContainerListItemBody = FC<ContainerListItemProps> { props ->
    div {
        className = ClassName("accordion-collapse collapse")
        id = "module-${props.id}-body"
        div {
            className = ClassName("accordion-body p-0")
            FileList {
                files = props.container.files ?: emptyList()
                sizeType = props.sizeType
            }
        }
    }
}

external interface FileListProps : Props {
    var files: List<AppFile>
    var sizeType: Measurable.SizeType
}

val FileList = FC<FileListProps> { props ->
    div {
        className = ClassName("list-group list-group-flush")
        props.files.forEach {
            FileListItem {
                file = it
                sizeType = props.sizeType
                key = it.name
            }
        }
    }
}

external interface FileListItemProps: Props {
    var file: AppFile
    var sizeType: Measurable.SizeType
}

val FileListItem = FC<FileListItemProps> { props ->
    div {
        className = ClassName("list-group-item d-flex border-0")
        span {
            className = ClassName("font-monospace text-truncate me-2")
            +props.file.name
        }
        span {
            className = ClassName("ms-auto me-custom text-nowrap")
            +formatSize(props.file, props.sizeType)
        }
    }
}
