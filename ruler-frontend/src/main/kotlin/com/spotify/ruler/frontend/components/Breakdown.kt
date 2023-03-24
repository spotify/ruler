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
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileContainer
import com.spotify.ruler.models.Measurable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun BreakDown(
    components: List<AppComponent>,
    sizeType: Measurable.SizeType
) {
    H4(attrs = {
        classes("mb-3")
    }) {
        Text("Breakdown (${components.size} components)")
    }
    Div(attrs = {
        classes("row")
    }) {
        ContainerList(
            containers = components,
            sizeType = sizeType
        )
    }
}

@Composable
fun ContainerList(containers: List<FileContainer>, sizeType: Measurable.SizeType) {
    Div(attrs = { classes("accordion") }) {
        containers.forEachIndexed { index, container ->
            ContainerListItem(
                id = index,
                container = container,
                sizeType = sizeType,
            )
        }
    }
}


@Composable
fun ContainerListItem(
    id: Int,
    container: FileContainer,
    sizeType: Measurable.SizeType
) {
    Div(attrs = {
        classes("accordion-item")
    }) {
        ContainerListItemHeader(
            id = id,
            container = container,
            sizeType = sizeType
        )
        ContainerListItemBody(
            id = id,
            container = container,
            sizeType = sizeType
        )
    }
}

@Composable
fun ContainerListItemHeader(
    id: Int,
    container: FileContainer,
    sizeType: Measurable.SizeType
) {
    val containsFiles = container.files != null
    H2(attrs = {
        classes("accordion-header")
    }) {
        val classes = mutableListOf("accordion-button", "collapsed")
        if (!containsFiles) {
            classes.add("disabled")
        }
        Button(
            attrs = {
                classes(classes)
                attr("data-bs-toggle", "collapse")
                attr("data-bs-target", "#module-$id-body")
            },
        ) {
            Span(attrs = {
                classes("font-monospace", "text-truncate", "me-3")
            }) {
                Text(container.name)
            }
            container.owner?.let { owner ->
                Span(attrs = {
                    classes("badge", "bg-secondary", "me-3")
                }) {
                    Text(owner)
                }
            }
            val sizeClasses = mutableListOf("ms-auto", "text-nowrap")
            if (containsFiles) {
                sizeClasses.add("me-3")
            }
            Span(attrs = {
                classes(sizeClasses)
            }) {
                Text(formatSize(container, sizeType))
            }
        }
    }
}

@Composable
fun ContainerListItemBody(
    id: Int,
    container: FileContainer,
    sizeType: Measurable.SizeType
) {
    Div(
        attrs = {
            id("module-$id-body")
            classes("accordion-collapse", "collapse")
        }
    ) {
        Div(attrs = {
            classes("accordion-body", "p-0")
        }) {
            FileList(container.files ?: emptyList(), sizeType)
        }
    }
}

@Composable
fun FileList(files: List<AppFile>, sizeType: Measurable.SizeType) {
    Div(attrs = {
        classes("list-group", "list-group-flush")
    }) {
        files.forEach { file ->
            FileListItem(file = file, sizeType = sizeType)
        }
    }
}


@Composable
fun FileListItem(file: AppFile, sizeType: Measurable.SizeType) {
    Div(attrs = {
        classes("list-group-item", "d-flex", "border-0")
    }) {
        Span(attrs = {
            classes("font-monospace", "text-truncate", "me-2")
        }) {
            Text(file.name)
        }
        Span(attrs = {
            classes("ms-auto", "me-custom", "text-nowrap")
        }) {
            Text(formatSize(file, sizeType))
        }
    }
}
