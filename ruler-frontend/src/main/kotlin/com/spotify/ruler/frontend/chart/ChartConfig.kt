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

package com.spotify.ruler.frontend.chart

import com.spotify.ruler.frontend.binding.ApexChartOptions
import kotlinext.js.jsObject

/** Base config for displaying charts. Check https://apexcharts.com/docs/options/ for all chart types and options. */
abstract class ChartConfig {

    /** Returns the chart options for this config used by ApexCharts. */
    abstract fun getOptions(): ApexChartOptions

    /** Utility function which allows concrete configs to start with a common sets of defaults. */
    protected fun buildOptions(builder: ApexChartOptions.() -> Unit) = jsObject<ApexChartOptions> {
        chart = jsObject {
            fontFamily = FONT_FAMILY
            toolbar = jsObject {
                show = false
            }
        }
        dataLabels = jsObject {
            enabled = false
        }
        fill = jsObject {
            opacity = 1.0
        }
        grid = jsObject {
            xaxis = jsObject {
                lines = jsObject()
            }
            yaxis = jsObject {
                lines = jsObject()
            }
        }
        legend = jsObject {
            fontSize = FONT_SIZE
            markers = jsObject {
                width = FONT_SIZE
                height = FONT_SIZE
            }
        }
        plotOptions = jsObject {
            bar = jsObject()
        }
        stroke = jsObject {
            show = true
            colors = arrayOf("transparent")
            width = STROKE_WIDTH
        }
        tooltip = jsObject {
            x = jsObject()
            y = jsObject()
        }
        xaxis = jsObject {
            labels = jsObject {
                style = jsObject {
                    fontSize = FONT_SIZE
                }
            }
        }
        yaxis = jsObject {
            labels = jsObject {
                style = jsObject {
                    fontSize = FONT_SIZE
                }
            }
        }
    }.apply(builder)

    private companion object {
        const val FONT_FAMILY = "var(--bs-body-font-family)"
        const val FONT_SIZE = 14
        const val STROKE_WIDTH = 3
    }
}
