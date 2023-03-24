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
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import kotlin.math.ceil
import kotlin.math.min


@Composable
fun PagedContent(
    itemCount: Int,
    pageSize: Int,
    content: @Composable (Int, Int) -> Unit
) {
    val pageCount = ceil(itemCount / pageSize.toFloat()).toInt()
    var activePage by remember {  mutableStateOf(1)  }
    val pageStartIndex = pageSize * (activePage - 1)
    val pageEndIndex = min(pageStartIndex + pageSize, itemCount)
    println("$pageStartIndex - $pageEndIndex")
    content(pageStartIndex, pageEndIndex)

    Ul(attrs = {
        classes("pagination", "justify-content-center")
    }) {
        for (page in 1..pageCount) {
            val classes = buildList<String> {
                add("page-item")
                if (page == activePage) {
                    add("active")
                }
            }
            Li(attrs = {
                classes(classes)

            }) {
                Button(attrs = {
                    classes("page-link")
                    onClick {
                        activePage = page
                    }
                }) {
                    Text(page.toString())
                }
            }
        }
    }
}
