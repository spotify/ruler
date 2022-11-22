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

import com.bnorm.react.RFunction
import com.bnorm.react.RKey
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileContainer
import com.spotify.ruler.models.Measurable
import kotlinx.html.id
import react.RBuilder
import react.dom.button
import react.dom.div
import react.dom.h2
import react.dom.h4
import react.dom.span

@RFunction
fun RBuilder.breakdown(components: List<AppComponent>, sizeType: Measurable.SizeType) {
    h4(classes = "mb-3") { +"Breakdown (${components.size} components)" }
    div(classes = "row") {
        containerList(components, sizeType)
    }
}

@RFunction
fun RBuilder.containerList(containers: List<FileContainer>, sizeType: Measurable.SizeType) {
    div(classes = "accordion") {
        containers.forEachIndexed { index, container ->
            containerListItem(index, container, sizeType, container.name)
        }
    }
}

@RFunction
@Suppress("UNUSED_PARAMETER")
fun RBuilder.containerListItem(id: Int, container: FileContainer, sizeType: Measurable.SizeType, @RKey key: String) {
    div(classes = "accordion-item") {
        containerListItemHeader(id, container, sizeType)
        containerListItemBody(id, container, sizeType)
    }
}

@RFunction
fun RBuilder.containerListItemHeader(id: Int, container: FileContainer, sizeType: Measurable.SizeType) {
    h2(classes = "accordion-header") {
        var classes = "accordion-button collapsed"
        if (container.files == null) {
            classes = "$classes disabled"
        }
        button(classes = classes) {
            attrs["data-bs-toggle"] = "collapse"
            attrs["data-bs-target"] = "#module-$id-body"
            span(classes = "font-monospace text-truncate me-3") { +container.name }
            container.owner?.let { owner -> span(classes = "badge bg-secondary me-3") { +owner } }
            span(classes = "ms-auto me-3 text-nowrap") {
                +formatSize(container, sizeType)
            }
        }
    }
}

@RFunction
fun RBuilder.containerListItemBody(id: Int, container: FileContainer, sizeType: Measurable.SizeType) {
    div(classes = "accordion-collapse collapse") {
        attrs.id = "module-$id-body"
        div(classes = "accordion-body p-0") {
            fileList(container.files ?: emptyList(), sizeType)
        }
    }
}

@RFunction
fun RBuilder.fileList(files: List<AppFile>, sizeType: Measurable.SizeType) {
    div(classes = "list-group list-group-flush") {
        files.forEach { file ->
            fileListItem(file, sizeType, file.name)
        }
    }
}

@RFunction
@Suppress("UNUSED_PARAMETER")
fun RBuilder.fileListItem(file: AppFile, sizeType: Measurable.SizeType, @RKey key: String) {
    div(classes = "list-group-item d-flex border-0") {
        span(classes = "font-monospace text-truncate me-2") { +file.name }
        span(classes = "ms-auto me-custom text-nowrap") {
            +formatSize(file, sizeType)
        }
    }
}
