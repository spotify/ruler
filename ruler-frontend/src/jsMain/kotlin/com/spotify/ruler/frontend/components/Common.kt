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
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.Measurable
import js.core.jso
import react.FC
import react.Props
import react.PropsWithChildren
import react.PropsWithClassName
import react.ReactNode
import react.asElementOrNull
import react.create
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul
import react.router.Outlet
import react.router.RouteObject
import react.router.RouterProvider
import react.router.dom.NavLink
import react.router.dom.createHashRouter
import react.useState
import web.cssom.ClassName

external interface ReportProps : Props {
    var report: AppReport
}

val Report = FC<ReportProps> { props ->
    val report = props.report

    val (sizeType, setSizeType) = useState(Measurable.SizeType.DOWNLOAD)

    val hasOwnershipInfo = report.components.any { component -> component.owner != null }
    val hasDynamicFeatures = report.dynamicFeatures.isNotEmpty()
    val hasFileLevelInfo = report.components.any { it.files != null }

    val tabs = listOf(
        Tab("/", "Breakdown") {
            Breakdown.create {
                components = report.components
                this.sizeType = sizeType
            }
        },
        Tab("/insights", "Insights") {
            Insights.create {
                components = report.components
                this.hasFileLevelInfo = hasFileLevelInfo
            }
        },
        Tab("/ownership", "Ownership", hasOwnershipInfo) {
            Ownership.create {
                components = report.components
                this.hasFileLevelInfo = hasFileLevelInfo
                this.sizeType = sizeType
            }
        },
        Tab(
            "/dynamic",
            "Dynamic features",
            hasDynamicFeatures
        ) {
            DynamicFeatures.create {
                features = report.dynamicFeatures
                this.sizeType = sizeType
            }
        },
    )


    val layout = div.create {
        className = ClassName("container mt-4 mb-5")
        div {
            className = ClassName("shadow-sm p-4 mb-4 bg-white rounded-1")
            Header {
                this@Header.report = report
            }
        }
        div {
            className = ClassName("shadow-sm p-4 bg-white rounded-1")

            Navigation {
                this@Navigation.tabs = tabs
                onSizeTypeSelected = { setSizeType(it) }

            }
            Outlet {

            }
        }
    }

    val hashRouter = createHashRouter(
        routes = arrayOf(jso {
            element = layout
            children = tabs.map {
                jso<RouteObject> {
                    path = it.path
                    element = it.content.invoke().asElementOrNull()
                }
            }.toTypedArray()
        })
    )

    RouterProvider {
        router = hashRouter
    }
}


val Header = FC<ReportProps> { props ->
    val report = props.report

    Row {
        Column {
            h3 { +report.name }
            span {
                className = ClassName("text-muted")

                +"Version ${report.version} (${report.variant})"
            }
        }
        HeaderSizeItem {
            size = report.downloadSize
            label = "Download size"
        }
        HeaderSizeItem {
            size = report.installSize
            label = "Install size"
        }
    }
}

external interface HeaderSizeItemProps : Props {
    var size: Number
    var label: String
}

val HeaderSizeItem = FC<HeaderSizeItemProps> { props ->
    div {
        className = ClassName("col-auto text-center ms-5 me-5")
        h3 { +formatSize(props.size) }
        span {
            className = ClassName("text-muted m-0")
            +props.label
        }
    }
}

external interface NavigationProps : Props {
    var tabs: List<Tab>
    var onSizeTypeSelected: (Measurable.SizeType) -> Unit
}

val Navigation = FC<NavigationProps> { props ->
    Row {

        className = ClassName("row align-items-center mb-4")
        Column {
            Tabs {
                tabs = props.tabs
            }
        }
        div {
            className = ClassName("col-auto")
            val optionMap = mapOf(
                "Download size" to Measurable.SizeType.DOWNLOAD,
                "Install size" to Measurable.SizeType.INSTALL,
            )
            DropDown {
                options = optionMap.keys
                id = "size-type-dropdown"
                onOptionSelected = { selectedOption ->
                    props.onSizeTypeSelected(optionMap.getValue(selectedOption))
                }
            }
        }
    }
}

external interface TabsProps : Props {
    var tabs: List<Tab>
}

val Tabs = FC<TabsProps> { props ->
    ul {
        className = ClassName("nav nav-pills")
        props.tabs.filter(Tab::enabled).forEach { (path, label) ->
            li {
                className = ClassName("nav-item")
                NavLink {
                    to = path
                    className = ClassName("nav-link")
                    +label
                }
            }
        }
    }
}

external interface DropDownProps : Props {
    var options: Iterable<String>
    var id: String
    var onOptionSelected: (String) -> Unit
}


val DropDown = FC<DropDownProps> { props ->
    select {
        className = ClassName("form-select")
        id = props.id
        onChange = { event ->
            props.onOptionSelected(event.target.value)
        }
        props.options.forEach { option ->
            option {
                value = option
                +option
            }
        }
    }
}

external interface PropsWithChildrenAndClassName : PropsWithChildren, PropsWithClassName

val Row = FC<PropsWithChildrenAndClassName> { props ->
    div {
        className = ClassName("row ${props.className ?: ""}")
        children = props.children
    }
}

val Column = FC<PropsWithChildrenAndClassName> { props ->
    div {
        className = ClassName("col ${props.className ?: ""}")
        children = props.children
    }
}

data class Tab(
    val path: String,
    val label: String,
    val enabled: Boolean = true,
    val content: () -> ReactNode
)
