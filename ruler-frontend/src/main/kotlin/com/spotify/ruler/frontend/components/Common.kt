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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.softwork.routingcompose.HashRouter
import app.softwork.routingcompose.Router
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.Measurable
import org.jetbrains.compose.web.attributes.href
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul


@Composable
fun Report(report: AppReport) {
    var sizeType: Measurable.SizeType by remember { mutableStateOf(Measurable.SizeType.DOWNLOAD) }

    val hasOwnershipInfo = report.components.any { component -> component.owner != null }
    val hasDynamicFeatures = report.dynamicFeatures.isNotEmpty()
    val hasFileLevelInfo = report.components.any { it.files != null }

    val tabs = listOf(
        Tab("/", "Breakdown"),
        Tab("/insights", "Insights"),
        Tab("/ownership", "Ownership", hasOwnershipInfo),
        Tab("/dynamic", "Dynamic features", hasDynamicFeatures),
    )

    Div(attrs = {
        classes("container", "mt-4", "mb-5")
    }) {
        Div(attrs = {
            classes("shadow-sm", "p-4", "mb-4", "bg-white", "rounded-1")
        }) {
            Header(report)
        }
        Div(attrs = {
            classes("shadow-sm", "p-4", "bg-white", "rounded-1")
        }) {
            HashRouter(initPath = "/") { // or BrowserRouter(initPath = "/hello") {
                Navigation(
                    tabs = tabs
                ) { sizeType = it }
                route("/") {
                    BreakDown(report.components, sizeType)
                }
                route("/insights") {
                    Insights(report.components, hasFileLevelInfo)
                }
                route("/ownership") {
                    Ownership(report.components, hasFileLevelInfo, sizeType)
                }
                route("/dynamic") {
                    DynamicFeatures(report.dynamicFeatures, sizeType)
                }
            }
        }
    }
}

@Composable
fun Header(report: AppReport) {
    Div(attrs = {
        classes("row")
    }) {
        Div(attrs = {
            classes("col")
        }) {
            H3 {
                Text(report.name)
            }
            Span(attrs = {
                classes("text-muted")
            }) {
                Text("Version ${report.version} (${report.variant})")
            }
        }
        HeaderSizeItem(report.downloadSize, "Download Size")
        HeaderSizeItem(report.installSize, "Install Size")
    }
}


@Composable
fun HeaderSizeItem(size: Number, label: String) {
    Div(attrs = {
        classes("col-auto", "text-center", "ms-5", "me-5")
    }) {
        H3 { Text(formatSize(size)) }
        Span(attrs = {
            classes("text-muted", "m-0")
        }) {
            Text(label)
        }
    }
}

@Composable
fun Navigation(tabs: List<Tab>, onSizeTypeSelected: (Measurable.SizeType) -> Unit) {
    Div(attrs = {
        classes("row", "align-items-center", "mb-4")
    }) {
        Div(attrs = {
            classes("col")
        }) {
            Tabs(tabs)
        }
        Div(attrs = {
            classes("col-auto")
        }) {
            val options = mapOf(
                "Download size" to Measurable.SizeType.DOWNLOAD,
                "Install size" to Measurable.SizeType.INSTALL,
            )
            Dropdown(
                id = "size-type-dropdown",
                options = options.keys,
                onOptionSelected = { selectedOption ->
                    onSizeTypeSelected(options.getValue(selectedOption))
                }
            )
        }
    }
}

@Composable
fun Tabs(tabs: List<Tab>) {
    Ul(attrs = {
        classes("nav", "nav-pills")
    }) {
        tabs.filter(Tab::enabled).forEach { (path, label) ->
            val router = Router.current
            val classes = buildList {
                add("nav-link")
                if (router.currentPath.path == path) {
                    add("active")
                }
            }
            Li(attrs = { classes("nav-item") }) {
                A(attrs = {
                    classes(classes)
                    onClick {
                        it.preventDefault()
                        router.navigate(to = path)
                    }
                    href("#")
                }) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
fun Dropdown(options: Iterable<String>, id: String, onOptionSelected: (String) -> Unit) {
    Select(attrs = {
        id(id)
        classes("form-select")
        onChange {
            onOptionSelected(it.target.value)
        }
    }) {
        options.forEach { option ->
            Option(option) {
                Text(option)
            }
        }
    }
}

data class Tab(
    val path: String,
    val label: String,
    val enabled: Boolean = true,
)
