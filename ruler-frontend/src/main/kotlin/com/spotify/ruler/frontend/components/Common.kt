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
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.Measurable
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLSelectElement
import react.RBuilder
import react.RProps
import react.dom.div
import react.dom.h3
import react.dom.li
import react.dom.option
import react.dom.select
import react.dom.span
import react.dom.ul
import react.router.dom.hashRouter
import react.router.dom.navLink
import react.router.dom.route
import react.router.dom.switch
import react.useState

@RFunction
fun RBuilder.report(report: AppReport) {
    val (sizeType, setSizeType) = useState(Measurable.SizeType.DOWNLOAD)

    val hasOwnershipInfo = report.components.any { component -> component.owner != null }

    val tabs = listOf(
        Tab("/", "Breakdown") { breakdown(report.components, sizeType) },
        Tab("/insights", "Insights") { insights(report.components) },
        Tab("/ownership", "Ownership", hasOwnershipInfo) { ownership(report.components, sizeType) },
    )

    div(classes = "container mt-4 mb-5") {
        div(classes = "shadow-sm p-4 mb-4 bg-white rounded-1") {
            header(report)
        }
        div(classes = "shadow-sm p-4 bg-white rounded-1") {
            hashRouter {
                navigation(tabs, onSizeTypeSelected = { setSizeType(it) })
                content(tabs)
            }
        }
    }
}

@RFunction
fun RBuilder.header(report: AppReport) {
    div(classes = "row") {
        div(classes = "col") {
            h3 { +report.name }
            span(classes = "text-muted") { +"Version ${report.version} (${report.variant})" }
        }
        headerSizeItem(report.downloadSize, "Download size")
        headerSizeItem(report.installSize, "Install size")
    }
}

@RFunction
fun RBuilder.headerSizeItem(size: Number, label: String) {
    div(classes = "col-auto text-center ms-5 me-5") {
        h3 { +formatSize(size) }
        span(classes = "text-muted m-0") { +label }
    }
}

@RFunction
fun RBuilder.navigation(tabs: List<Tab>, onSizeTypeSelected: (Measurable.SizeType) -> Unit) {
    div(classes = "row align-items-center mb-4") {
        div(classes = "col") {
            tabs(tabs)
        }
        div(classes = "col-auto") {
            val options = mapOf(
                "Download size" to Measurable.SizeType.DOWNLOAD,
                "Install size" to Measurable.SizeType.INSTALL,
            )
            dropdown(options, onSizeTypeSelected)
        }
    }
}

@RFunction
fun RBuilder.tabs(tabs: List<Tab>) {
    ul(classes = "nav nav-pills") {
        tabs.filter(Tab::enabled).forEach { (path, label) ->
            li(classes = "nav-item") {
                navLink<RProps>(to = path, className = "nav-link", exact = true) { +label }
            }
        }
    }
}

@RFunction
fun RBuilder.content(tabs: List<Tab>) {
    switch {
        tabs.forEach { (path, _, _, content) ->
            route(path, exact = true, render = content)
        }
    }
}

@RFunction
fun <T : Enum<T>> RBuilder.dropdown(options: Map<String, T>, onOptionSelected: (T) -> Unit) {
    dropdown(options.keys) { selectedOption ->
        onOptionSelected(options.getValue(selectedOption))
    }
}

@RFunction
fun RBuilder.dropdown(options: Iterable<String>, onOptionSelected: (String) -> Unit) {
    select(classes = "form-select") {
        attrs.onChangeFunction = { event ->
            onOptionSelected((event.target as HTMLSelectElement).value)
        }
        options.forEach { option ->
            option {
                attrs.value = option
                +option
            }
        }
    }
}

data class Tab(
    val path: String,
    val label: String,
    val enabled: Boolean = true,
    val content: RBuilder.() -> Unit
)
