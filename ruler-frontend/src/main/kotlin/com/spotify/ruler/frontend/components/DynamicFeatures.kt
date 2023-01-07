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
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.Measurable
import react.RBuilder
import react.dom.div
import react.dom.h4
import react.table.columns
import react.table.useTable
import react.table.TableInstance
import kotlinx.js.jso
import com.spotify.ruler.models.FileContainer

private val COLUMNS = columns<DynamicFeature> {
    column<String> {
        header = "Name"
        accessor = "name"
    }
}

@RFunction
fun RBuilder.dynamicFeatures(features: List<DynamicFeature>, sizeType: Measurable.SizeType) {
    h4(classes = "mb-3") { +"Dynamic features" }

    val table = useTable<DynamicFeature>(
        options = jso {
            data = features.toTypedArray()
            columns = COLUMNS
        }
    );

    table.getTableProps()
    table.getTableBodyProps()

    div(classes = "row") {
        containerList(table as TableInstance<FileContainer>, sizeType)
    }

    //
    // div(classes = "row") {
    //     containerList(features, sizeType)
    // }
}
