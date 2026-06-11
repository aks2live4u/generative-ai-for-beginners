package com.prettycountdown.util

import com.prettycountdown.data.model.CountdownFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** A breakdown of the time between "now" and an event's target date/time. */
data class CountdownBreakdown(
    val totalMillis: Long,
    val totalDays: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val isPast: Boolean
)

/**
 * All the math behind every countdown widget and screen. Pure functions only -
 * this is what makes the same logic reusable from Compose, Glance and WorkManager.
 */
object CountdownMath {
    private const val MS_PER_MINUTE = 60_000L
    private const val MS_PER_HOUR = 60 * MS_PER_MINUTE
    private const val MS_PER_DAY = 24 * MS_PER_HOUR

    fun breakdown(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): CountdownBreakdown {
        val diff = targetMillis - nowMillis
        val isPast = diff < 0
        val absDiff = abs(diff)
        val totalDays = absDiff / MS_PER_DAY
        val remainder = absDiff % MS_PER_DAY
        val hours = remainder / MS_PER_HOUR
        val minutes = (remainder % MS_PER_HOUR) / MS_PER_MINUTE
        val seconds = (remainder % MS_PER_MINUTE) / 1000L
        return CountdownBreakdown(diff, totalDays, hours, minutes, seconds, isPast)
    }

    /** Days remaining, rounded up so "23 hours from now" still reads as "1 day". */
    fun daysRemaining(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        val diff = targetMillis - nowMillis
        if (diff <= 0) return 0
        return ceil(diff / MS_PER_DAY.toDouble()).toLong()
    }

    /** Fraction (0f..1f) of the journey from [startMillis] to [targetMillis] completed. */
    fun progressFraction(startMillis: Long, targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Float {
        if (targetMillis <= startMillis) return 1f
        val total = (targetMillis - startMillis).toFloat()
        val elapsed = (nowMillis - startMillis).toFloat()
        return (elapsed / total).coerceIn(0f, 1f)
    }

    /** The big headline number shown on a widget, e.g. "245". */
    fun primaryValue(format: CountdownFormat, b: CountdownBreakdown, progress: Float): String {
        if (b.isPast) return "0"
        return when (format) {
            CountdownFormat.DAYS_ONLY,
            CountdownFormat.SLEEPS_LEFT -> ceil(b.totalDays + b.hours / 24f).toLong().coerceAtLeast(0).toString()
            CountdownFormat.DAYS_HOURS -> b.totalDays.toString()
            CountdownFormat.DAYS_HOURS_MINUTES -> b.totalDays.toString()
            CountdownFormat.WEEKS_REMAINING -> ceil(b.totalDays / 7.0).toInt().toString()
            CountdownFormat.PERCENTAGE_COMPLETE -> (progress * 100).roundToInt().toString()
        }
    }

    /** The supporting label/suffix shown alongside [primaryValue], e.g. "Days Left". */
    fun unitLabel(format: CountdownFormat, b: CountdownBreakdown): String = when (format) {
        CountdownFormat.DAYS_ONLY -> if (b.totalDays == 1L) "Day Left" else "Days Left"
        CountdownFormat.DAYS_HOURS -> "d ${b.hours}h left"
        CountdownFormat.DAYS_HOURS_MINUTES -> "d ${b.hours}h ${b.minutes}m left"
        CountdownFormat.SLEEPS_LEFT -> if (b.totalDays == 1L) "Sleep Left" else "Sleeps Left"
        CountdownFormat.WEEKS_REMAINING -> "Weeks Remaining"
        CountdownFormat.PERCENTAGE_COMPLETE -> "Complete"
    }

    /** A single human-readable line, e.g. "245d 12h 31m" or "78% Complete". */
    fun formatLine(format: CountdownFormat, targetMillis: Long, startMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val b = breakdown(targetMillis, nowMillis)
        if (b.isPast) return "Happening now!"
        val progress = progressFraction(startMillis, targetMillis, nowMillis)
        return when (format) {
            CountdownFormat.DAYS_ONLY -> "${b.totalDays} ${if (b.totalDays == 1L) "Day" else "Days"}"
            CountdownFormat.DAYS_HOURS -> "${b.totalDays}d ${b.hours}h"
            CountdownFormat.DAYS_HOURS_MINUTES -> "${b.totalDays}d ${b.hours}h ${b.minutes}m"
            CountdownFormat.SLEEPS_LEFT -> "${b.totalDays} ${if (b.totalDays == 1L) "Sleep" else "Sleeps"}"
            CountdownFormat.WEEKS_REMAINING -> "${ceil(b.totalDays / 7.0).toInt()} Weeks"
            CountdownFormat.PERCENTAGE_COMPLETE -> "${(progress * 100).roundToInt()}% Complete"
        }
    }

    fun formatDate(millis: Long, hasTime: Boolean): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val pattern = if (hasTime) "d MMMM yyyy, h:mm a" else "d MMMM yyyy"
        return zdt.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun formatShortDate(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return zdt.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}
