package com.dosemate.analytics

import com.dosemate.data.LogStatus
import com.dosemate.data.MedicineLog
import java.time.LocalDate
import java.time.LocalTime

object InsightsEngine {

    fun generate(logs: List<MedicineLog>): List<String> {
        val resolved = logs.filter { it.status != LogStatus.PENDING }
        if (resolved.isEmpty()) return listOf("Add a medicine and confirm a few doses to unlock insights.")

        val insights = mutableListOf<String>()

        val stats = AdherenceCalculator.computeStats(logs)
        insights += "Your adherence is ${stats.adherencePercent}% this month."

        val (weekdayPct, weekendPct) = AdherenceCalculator.weekdayVsWeekendAdherence(logs)
        if (weekdayPct != weekendPct) {
            val better = if (weekdayPct >= weekendPct) "weekdays" else "weekends"
            insights += "You're most consistent on $better."
        }

        val halves = splitDelayTrend(resolved)
        if (halves != null) {
            val (first, second) = halves
            if (first > second) {
                insights += "Average medicine delay reduced from $first minutes to $second minutes."
            } else if (second > first) {
                insights += "Average medicine delay increased from $first minutes to $second minutes."
            }
        }

        val eveningMissRate = missRateForWindow(resolved, LocalTime.of(17, 0), LocalTime.of(23, 59))
        val morningMissRate = missRateForWindow(resolved, LocalTime.of(0, 0), LocalTime.of(11, 59))
        if (eveningMissRate > morningMissRate && eveningMissRate > 0) {
            insights += "You tend to miss evening doses."
        } else if (morningMissRate > eveningMissRate && morningMissRate > 0) {
            insights += "You tend to miss morning doses."
        }

        if (stats.currentStreakDays >= 3) {
            insights += "You're on a ${stats.currentStreakDays}-day streak. Keep it going!"
        }

        return insights
    }

    private fun splitDelayTrend(resolved: List<MedicineLog>): Pair<Int, Int>? {
        val taken = resolved.filter { it.status == LogStatus.TAKEN }
            .sortedBy { it.dateEpochDay }
        if (taken.size < 4) return null
        val mid = taken.size / 2
        val firstHalf = taken.subList(0, mid).mapNotNull { it.delayMinutes }
        val secondHalf = taken.subList(mid, taken.size).mapNotNull { it.delayMinutes }
        if (firstHalf.isEmpty() || secondHalf.isEmpty()) return null
        return (firstHalf.average().toInt()) to (secondHalf.average().toInt())
    }

    private fun missRateForWindow(logs: List<MedicineLog>, start: LocalTime, end: LocalTime): Float {
        val inWindow = logs.filter {
            val time = java.time.Instant.ofEpochMilli(it.scheduledEpochMillis)
                .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            !time.isBefore(start) && !time.isAfter(end)
        }
        if (inWindow.isEmpty()) return 0f
        return inWindow.count { it.status == LogStatus.MISSED } * 100f / inWindow.size
    }
}
