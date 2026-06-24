package com.dosemate.analytics

import com.dosemate.data.LogStatus
import com.dosemate.data.MedicineLog
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object AdherenceCalculator {

    fun computeStats(logs: List<MedicineLog>): AdherenceStats {
        val resolved = logs.filter { it.status != LogStatus.PENDING }
        val taken = resolved.count { it.status == LogStatus.TAKEN }
        val missed = resolved.count { it.status == LogStatus.MISSED }
        val total = resolved.size
        val adherence = if (total == 0) 100 else (taken * 100 / total)
        val avgDelay = takenAverageDelay(resolved)
        val streak = currentStreakDays(logs)
        return AdherenceStats(
            adherencePercent = adherence,
            averageDelayMinutes = avgDelay,
            currentStreakDays = streak,
            missedCount = missed,
            takenCount = taken,
            totalCount = total
        )
    }

    fun takenAverageDelay(logs: List<MedicineLog>): Int {
        val delays = logs.filter { it.status == LogStatus.TAKEN }.mapNotNull { it.delayMinutes }
        return if (delays.isEmpty()) 0 else delays.sum() / delays.size
    }

    /** Consecutive days (most recent first) where every scheduled dose was taken. */
    fun currentStreakDays(logs: List<MedicineLog>): Int {
        val byDay = logs.filter { it.status != LogStatus.PENDING }
            .groupBy { it.dateEpochDay }
        if (byDay.isEmpty()) return 0
        var day = LocalDate.now().toEpochDay()
        var streak = 0
        while (true) {
            val dayLogs = byDay[day] ?: break
            if (dayLogs.isEmpty()) break
            val allTaken = dayLogs.all { it.status == LogStatus.TAKEN }
            if (!allTaken) break
            streak++
            day--
        }
        return streak
    }

    fun timingTrend(logs: List<MedicineLog>): List<TrendPoint> {
        return logs.filter { it.status == LogStatus.TAKEN }
            .groupBy { it.dateEpochDay }
            .toSortedMap()
            .map { (day, dayLogs) ->
                val avg = dayLogs.mapNotNull { it.delayMinutes }.average().toFloat()
                val date = LocalDate.ofEpochDay(day)
                TrendPoint("${date.monthValue}/${date.dayOfMonth}", avg)
            }
    }

    fun weeklyAdherenceTrend(logs: List<MedicineLog>): List<TrendPoint> {
        val byWeek = logs.filter { it.status != LogStatus.PENDING }
            .groupBy { LocalDate.ofEpochDay(it.dateEpochDay).let { d -> d.year * 100 + d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()) } }
        return byWeek.toSortedMap().entries.mapIndexed { index, entry ->
            val taken = entry.value.count { it.status == LogStatus.TAKEN }
            val pct = if (entry.value.isEmpty()) 0f else taken * 100f / entry.value.size
            TrendPoint("W${index + 1}", pct)
        }
    }

    fun medicineStats(medicineName: String, logs: List<MedicineLog>): MedicineStats {
        val resolved = logs.filter { it.status != LogStatus.PENDING }
        val missed = resolved.count { it.status == LogStatus.MISSED }
        val avgDelay = takenAverageDelay(resolved)
        val adherence = if (resolved.isEmpty()) -1 else resolved.count { it.status == LogStatus.TAKEN } * 100 / resolved.size
        val takenTimeLabel = usualTakenTimeLabel(resolved)
        return MedicineStats(medicineName, avgDelay, missed, adherence, takenTimeLabel)
    }

    /** Average clock time at which doses were actually taken, e.g. "9:05 AM". Null if never taken. */
    fun usualTakenTimeLabel(logs: List<MedicineLog>): String? {
        val takenTimes = logs.filter { it.status == LogStatus.TAKEN }
            .mapNotNull { it.actualTakenEpochMillis }
        if (takenTimes.isEmpty()) return null
        val zone = java.time.ZoneId.systemDefault()
        val minutesOfDay = takenTimes.map {
            val zoned = java.time.Instant.ofEpochMilli(it).atZone(zone)
            zoned.hour * 60 + zoned.minute
        }
        val avgMinutes = minutesOfDay.sum() / minutesOfDay.size
        val time = java.time.LocalTime.of(avgMinutes / 60, avgMinutes % 60)
        return time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
    }

    fun weekdayVsWeekendAdherence(logs: List<MedicineLog>): Pair<Int, Int> {
        val resolved = logs.filter { it.status != LogStatus.PENDING }
        val weekday = resolved.filter {
            val dow = LocalDate.ofEpochDay(it.dateEpochDay).dayOfWeek
            dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY
        }
        val weekend = resolved.filter {
            val dow = LocalDate.ofEpochDay(it.dateEpochDay).dayOfWeek
            dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY
        }
        fun pct(list: List<MedicineLog>) = if (list.isEmpty()) 100 else list.count { it.status == LogStatus.TAKEN } * 100 / list.size
        return pct(weekday) to pct(weekend)
    }

    fun dayLabel(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}
