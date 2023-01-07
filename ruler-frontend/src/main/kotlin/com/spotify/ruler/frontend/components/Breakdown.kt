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
import kotlinx.js.jso
import react.RBuilder
import react.dom.button
import react.dom.div
import react.dom.h2
import react.dom.h4
import react.dom.span
import react.table.columns
import react.table.useTable
import react.table.RenderType
import react.table.usePagination
import react.table.TableInstance
import react.table.Cell
import react.table.TableCell

private val COMPONENTS_COLUMNS = columns<AppComponent> {
    column<String> {
        accessorFunction = { it.name }
        id = "name"
    }

    column<String?> {
        accessorFunction = { it.owner }
        id = "owner"
    }

    column<Long> {
        accessorFunction = { it.downloadSize }
        id = "downloadSize"
    }

    column<Long> {
        accessorFunction = { it.installSize }
        id = "installSize"
    }
}

private val FILE_LIST_COLUMNS = columns<AppFile> {
    column<String> {
        accessorFunction = { it.name }
        id = "name"
    }

    column<Long> {
        accessorFunction = { it.downloadSize }
        id = "downloadSize"
    }

    column<Long> {
        accessorFunction = { it.installSize }
        id = "installSize"
    }
}

@RFunction
fun RBuilder.breakdown(components: List<AppComponent>, sizeType: Measurable.SizeType) {
    h4(classes = "mb-3") { +"Breakdown (${components.size} components)" }

    val table = useTable<AppComponent>(
        options = jso {
            data = components.toTypedArray()
            columns = COMPONENTS_COLUMNS
            // initialState = jso {
            //     pageSize = 10
            // }
        }
    );

    table.getTableProps()
    table.getTableBodyProps()

    div(classes = "row") {
        containerList(table as TableInstance<FileContainer>, sizeType)
    }
}


@RFunction
fun RBuilder.containerList(table: TableInstance<FileContainer>, sizeType: Measurable.SizeType) {
    div(classes = "accordion") {
        table.rows.mapIndexed { index, row ->
            table.prepareRow(row)
            row.getRowProps()

            div(classes = "accordion-item") {
                h2(classes = "accordion-header") {
                    var classes = "accordion-button collapsed"

                    val nameCell: TableCell<FileContainer, *>? = row.cells.find {
                        it.getCellProps()
                        it.column.id == "name"
                    }
                    val ownerCell: TableCell<FileContainer, *>? = row.cells.find {
                        it.getCellProps()
                        it.column.id == "owner"
                    }

                    div(classes = classes) {
                        attrs["data-bs-toggle"] = "collapse"
                        attrs["data-bs-target"] = "#module-$index-body"

                        span(classes = "font-monospace text-truncate me-3") { nameCell?.render(RenderType.Cell)?.unaryPlus() }
                        span(classes = "badge bg-secondary me-3") { ownerCell?.render(RenderType.Cell)?.unaryPlus() }

                        var sizeClasses = "ms-auto text-nowrap"
                        span(classes = sizeClasses) {
                            +formatSize(row.original, sizeType)
                        }
                    }
                }
            }
        }
    }
}

// @RFunction
// fun RBuilder.containerList(table: TableInstance<FileContainer>, sizeType: Measurable.SizeType) {
//     div(classes = "accordion") {
//         table.rows.mapIndexed { index, row ->
//             table.prepareRow(row)
//             row.getRowProps()
//             containerListItem(index, row.original, sizeType, row.original.name)
//         }
//     }
// }

//
// @RFunction
// fun RBuilder.containerList(containers: List<FileContainer>, sizeType: Measurable.SizeType) {
//     div(classes = "accordion") {
//         containers.forEachIndexed { index, container ->
//             containerListItem(index, container, sizeType, container.name)
//         }
//     }
// }

@RFunction
@Suppress("UNUSED_PARAMETER")
fun RBuilder.containerListItem(id: Int, container: FileContainer, sizeType: Measurable.SizeType, @RKey key: String) {
    div(classes = "accordion-item") {
        containerListItemHeader(id, container, sizeType)
        //containerListItemBody(id, container, sizeType)
    }
}

@RFunction
fun RBuilder.containerListItemHeader(id: Int, container: FileContainer, sizeType: Measurable.SizeType) {
    val containsFiles = container.files != null
    h2(classes = "accordion-header") {
        var classes = "accordion-button collapsed"
        if (!containsFiles) {
            classes = "$classes disabled"
        }
        button(classes = classes) {
            attrs["data-bs-toggle"] = "collapse"
            attrs["data-bs-target"] = "#module-$id-body"
            span(classes = "font-monospace text-truncate me-3") { +container.name }
            container.owner?.let { owner -> span(classes = "badge bg-secondary me-3") { +owner } }
            var sizeClasses = "ms-auto text-nowrap"
            if (containsFiles) {
                sizeClasses = "$sizeClasses me-3"
            }
            span(classes = sizeClasses) {
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
    val table = useTable<AppFile>(
        options = jso {
            data = files.toTypedArray()
            columns = FILE_LIST_COLUMNS
            initialState = jso {
                pageSize = 10000
            }
        },
        usePagination
    );

    table.getTableProps()
    table.getTableBodyProps()

    div(classes = "list-group list-group-flush") {
        table.page.mapIndexed { index, row ->
            //if (index <= 10) {
                table.prepareRow(row)
                row.getRowProps()

                //fileListItem(row, sizeType)

                val nameCell: TableCell<AppFile, *>? = row.cells!!.find {
                    it.getCellProps()
                    it.column.id == "name"
                }
                val sizeCell: TableCell<AppFile, *>? = row.cells!!.find {
                    it.getCellProps()
                    it.column.id == "downloadSize"
                }

                div(classes = "list-group-item d-flex border-0") {
                    span(classes = "font-monospace text-truncate me-2") { +nameCell!!.render(RenderType.Cell) }
                    span(classes = "ms-auto me-custom text-nowrap") {
                        //+formatSize(file, sizeType)
                        +sizeCell!!.render(RenderType.Cell)
                    }
                }
            // row.cells.map{ cell ->
            //         span(classes = "font-monospace text-truncate me-2") {
            //             cell.getCellProps()
            //             +cell!!.render(RenderType.Cell)
            //         }
            // }
            //}
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

// @RFunction
// @Suppress("UNUSED_PARAMETER")
// fun RBuilder.fileListItem(file: AppFile, sizeType: Measurable.SizeType, @RKey key: String) {
//     div(classes = "list-group-item d-flex border-0") {
//         span(classes = "font-monospace text-truncate me-2") { +file.name }
//         span(classes = "ms-auto me-custom text-nowrap") {
//             +formatSize(file, sizeType)
//         }
//     }
// }
