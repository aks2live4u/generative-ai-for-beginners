package com.finsight.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A category slice in [DonutChart]: [value] in any consistent unit, rendered proportionally. */
data class DonutSlice(val label: String, val value: Double, val color: Color)

/**
 * Two-line trend chart (income vs. expense) drawn with Canvas - avoids pulling in a third-party
 * charting library for what's a simple dual polyline over N points.
 */
@Composable
fun TrendLineChart(
    incomeSeries: List<Double>,
    expenseSeries: List<Double>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val maxValue = (incomeSeries + expenseSeries).maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val pointCount = maxOf(incomeSeries.size, expenseSeries.size, 2)
        val stepX = size.width / (pointCount - 1).coerceAtLeast(1)

        fun drawSeries(series: List<Double>, color: Color) {
            if (series.size < 2) return
            val path = androidx.compose.ui.graphics.Path()
            series.forEachIndexed { index, value ->
                val x = stepX * index
                val y = size.height - (value / maxValue * size.height).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }

        drawSeries(incomeSeries, incomeColor)
        drawSeries(expenseSeries, expenseColor)
    }
}

/** Donut/pie chart over [slices], proportional by [DonutSlice.value]. Renders as concentric arcs. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 28f
) {
    Canvas(modifier = modifier) {
        val total = slices.sumOf { it.value }.takeIf { it > 0 } ?: return@Canvas
        val strokeWidth = strokeWidthDp
        var startAngle = -90f
        val diameter = minOf(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

        slices.forEach { slice ->
            val sweep = (slice.value / total * 360f).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

/** Circular progress ring used for the Financial Health Score gauge (e.g. "82/100"). */
@Composable
fun ScoreRing(
    score: Int,
    maxScore: Int = 100,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 14f
) {
    Canvas(modifier = modifier) {
        val diameter = minOf(size.width, size.height) - strokeWidthDp
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthDp, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        val sweep = (score.toFloat() / maxScore.toFloat()) * 360f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthDp, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}
