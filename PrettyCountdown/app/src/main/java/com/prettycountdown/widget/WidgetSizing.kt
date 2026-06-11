package com.prettycountdown.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The widget sizes Pretty Countdown supports, matching the small (2x2), medium (4x2)
 * and large (4x4) presets from the design spec. Glance picks the closest of these
 * via [androidx.glance.appwidget.SizeMode.Responsive].
 */
object WidgetSizes {
    val SMALL = DpSize(110.dp, 110.dp)
    val MEDIUM = DpSize(250.dp, 110.dp)
    val LARGE = DpSize(250.dp, 250.dp)

    val responsive = setOf(SMALL, MEDIUM, LARGE)
}

enum class WidgetSizeBucket { SMALL, MEDIUM, LARGE }

fun bucketFor(size: DpSize): WidgetSizeBucket = when {
    size.height < 130.dp && size.width < 180.dp -> WidgetSizeBucket.SMALL
    size.height < 180.dp -> WidgetSizeBucket.MEDIUM
    else -> WidgetSizeBucket.LARGE
}
