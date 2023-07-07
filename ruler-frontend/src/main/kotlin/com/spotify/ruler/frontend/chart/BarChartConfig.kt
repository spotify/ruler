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

import com.spotify.ruler.frontend.binding.NumberFormatter
import com.spotify.ruler.frontend.binding.Series
import com.spotify.ruler.frontend.binding.TooltipAxisFormatterOptions
import com.spotify.ruler.frontend.formatPercentage

/** Chart config for bar charts. */
@Suppress("LongParameterList")
class BarChartConfig(
    private val chartLabels: Array<String>,
    private val chartSeries: Array<Series>,
    private val chartHeight: Int,
    private val horizontal: Boolean = false,
    private val xAxisFormatter: NumberFormatter = Number::toString,
    private val yAxisFormatter: NumberFormatter = Number::toString,
    private val chartSeriesTotals: LongArray? = null,
) : ChartConfig() {

    override fun getOptions() = buildOptions {
        series = chartSeries
        xaxis.categories = chartLabels
        chart.type = "bar"
        chart.height = chartHeight

        grid.xaxis.lines.show = horizontal
        grid.yaxis.lines.show = !horizontal
        plotOptions.bar.horizontal = horizontal

        xaxis.labels.formatter = xAxisFormatter
        yaxis.labels.formatter = yAxisFormatter
        tooltip.y.formatter = ::formatTooltip
    }

    private fun formatTooltip(number: Number, options: TooltipAxisFormatterOptions): String {
        val axisFormatter = if (horizontal) xAxisFormatter else yAxisFormatter
        val total = if (chartSeriesTotals != null) {
            chartSeriesTotals[options.seriesIndex]
        } else {
            options.series[options.seriesIndex].sumOf(Number::toLong)
        }
        return "${axisFormatter.invoke(number)} (${formatPercentage(number, total)})"
    }
}
