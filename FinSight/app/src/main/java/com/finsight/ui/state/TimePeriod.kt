package com.finsight.ui.state

import java.time.LocalDate
import java.time.temporal.WeekFields

/** User-selectable reporting window for the Dashboard and Transactions screens. */
enum class TimePeriod(val label: String, val comparisonLabel: String) {
    WEEK("This Week", "last week"),
    MONTH("This Month", "last month"),
    YEAR("This Year", "last year"),
    ALL_TIME("All Time", "");

    fun currentRange(now: LocalDate): ClosedRange<LocalDate> = when (this) {
        WEEK -> now.with(WeekFields.ISO.dayOfWeek(), 1L)..now
        MONTH -> now.withDayOfMonth(1)..now
        YEAR -> now.withDayOfYear(1)..now
        ALL_TIME -> LocalDate.MIN..now
    }

    /** Null for [ALL_TIME] - there's no meaningful "previous all time" to compare against. */
    fun previousRange(now: LocalDate): ClosedRange<LocalDate>? = when (this) {
        WEEK -> {
            val start = now.with(WeekFields.ISO.dayOfWeek(), 1L).minusWeeks(1)
            start..start.plusDays(6)
        }
        MONTH -> {
            val start = now.withDayOfMonth(1).minusMonths(1)
            start..start.withDayOfMonth(start.lengthOfMonth())
        }
        YEAR -> {
            val start = now.withDayOfYear(1).minusYears(1)
            start..start.withDayOfYear(start.lengthOfYear())
        }
        ALL_TIME -> null
    }
}
