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
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.button
import react.dom.li
import react.dom.ul
import react.useState
import kotlin.math.ceil
import kotlin.math.min

@RFunction
fun RBuilder.pagedContent(itemCount: Int, pageSize: Int, content: RBuilder.(Int, Int) -> Unit) {
    val pageCount = ceil(itemCount / pageSize.toFloat()).toInt()
    var activePage by useState(1)

    val pageStartIndex = pageSize * (activePage - 1)
    val pageEndIndex = min(pageStartIndex + pageSize, itemCount)
    content.invoke(this, pageStartIndex, pageEndIndex)

    ul(classes = "pagination justify-content-center") {
        for (page in 1..pageCount) {
            val activeClass = if (page == activePage) "active" else ""
            li(classes = "page-item $activeClass") {
                button(classes = "page-link") {
                    attrs.onClickFunction = { activePage = page }
                    +page.toString()
                }
            }
        }
    }
}
