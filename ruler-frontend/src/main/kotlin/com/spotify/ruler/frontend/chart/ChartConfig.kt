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
import kotlinx.js.jso

/** Base config for displaying charts. Check https://apexcharts.com/docs/options/ for all chart types and options. */
abstract class ChartConfig {

    /** Returns the chart options for this config used by ApexCharts. */
    abstract fun getOptions(): ApexChartOptions

    /** Utility function which allows concrete configs to start with a common sets of defaults. */
    protected fun buildOptions(builder: ApexChartOptions.() -> Unit) = jso<ApexChartOptions> {
        chart = jso {
            fontFamily = FONT_FAMILY
            toolbar = jso {
                show = false
            }
        }
        dataLabels = jso {
            enabled = false
        }
        fill = jso {
            opacity = 1.0
        }
        grid = jso {
            xaxis = jso {
                lines = jso()
            }
            yaxis = jso {
                lines = jso()
            }
        }
        legend = jso {
            fontSize = FONT_SIZE
            markers = jso {
                width = FONT_SIZE
                height = FONT_SIZE
            }
        }
        plotOptions = jso {
            bar = jso()
        }
        stroke = jso {
            show = true
            colors = arrayOf("transparent")
            width = STROKE_WIDTH
        }
        tooltip = jso {
            x = jso()
            y = jso()
        }
        xaxis = jso {
            labels = jso {
                style = jso {
                    fontSize = FONT_SIZE
                }
            }
        }
        yaxis = jso {
            labels = jso {
                style = jso {
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
