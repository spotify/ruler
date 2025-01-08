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

import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName

val PageControl = FC<PageControlProps> { props ->
    ul {
        className = ClassName("pagination justify-content-center")
        for (page in 1..props.pageCount) {
            val activeClass = if (page == props.activePage) "active" else ""
            li {
                className = ClassName("page-item $activeClass")
                button {
                    className = ClassName("page-link")
                    onClick = { props.onChangePage(page) }
                    +page.toString()
                }
            }
        }
    }
}

external interface PageControlProps : Props {
    var pageCount: Int
    var activePage: Int
    var onChangePage: (Int) -> Unit
}
