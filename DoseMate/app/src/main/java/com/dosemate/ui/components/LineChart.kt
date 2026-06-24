package com.dosemate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosemate.analytics.TrendPoint
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.ui.theme.TealPrimary

@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    lineColor: Color = TealPrimary,
    modifier: Modifier = Modifier
) {
    val colors = LocalDoseMateColors.current

    if (points.size < 2) {
        Box(modifier.fillMaxWidth().height(160.dp)) {
            Text("Not enough data yet", color = colors.textSecondary, fontSize = 13.sp)
        }
        return
    }

    val maxVal = points.maxOf { it.value }.coerceAtLeast(1f)
    val minVal = points.minOf { it.value }.coerceAtMost(0f)
    val range = (maxVal - minVal).coerceAtLeast(1f)
    val baselineColor = colors.textSecondary.copy(alpha = 0.3f)

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        // zero baseline guide
        val zeroY = h - ((0f - minVal) / range) * h
        drawLine(
            color = baselineColor,
            start = androidx.compose.ui.geometry.Offset(0f, zeroY),
            end = androidx.compose.ui.geometry.Offset(w, zeroY),
            strokeWidth = 1.5f
        )

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = h - ((point.value - minVal) / range) * h
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = h - ((point.value - minVal) / range) * h
            drawCircle(color = lineColor, radius = 7f, center = androidx.compose.ui.geometry.Offset(x, y))
            drawCircle(color = Color.White, radius = 3f, center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}
