package com.prettycountdown.widget

import kotlin.math.roundToInt

/** Small text-art helpers that stand in for the richer Compose drawing the
 * full app uses, since Glance widgets can't render arbitrary Canvas shapes. */
object WidgetVisuals {

    /** A row of [segments] dots showing how much of [progress] (0f..1f) is complete. */
    fun progressDots(progress: Float, segments: Int = 5): String {
        val clamped = progress.coerceIn(0f, 1f)
        val filled = clamped * segments
        return (0 until segments).joinToString(" ") { index ->
            when {
                index + 1 <= filled -> "●"
                index < filled -> "◐"
                else -> "○"
            }
        }
    }

    /** A "🏠 ··🚶·· 🎯" style track showing a traveler walking toward [destinationEmoji]. */
    fun journeyTrack(progress: Float, destinationEmoji: String, length: Int = 9): String {
        val clamped = progress.coerceIn(0f, 1f)
        val walkerPosition = (clamped * length).roundToInt().coerceIn(0, length)
        return buildString {
            append("🏠")
            repeat(length) { index -> append(if (index == walkerPosition) "🚶" else "·") }
            append(destinationEmoji)
        }
    }
}
