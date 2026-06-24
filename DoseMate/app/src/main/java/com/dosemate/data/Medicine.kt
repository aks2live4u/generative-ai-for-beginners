package com.dosemate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * specificDaysCsv: comma-separated java.time.DayOfWeek ordinal values, only used when
 * frequency == SPECIFIC_DAYS. everyXHours is only used when frequency == EVERY_X_HOURS.
 * timesCsv: comma-separated "HH:mm" dose times for DAILY/SPECIFIC_DAYS/ALTERNATE_DAYS medicines
 * that are taken more than once a day. Empty means just the single reminderHour/reminderMinute.
 */
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val medicineId: Long = 0,
    val name: String,
    val dosage: String,
    val reminderHour: Int,
    val reminderMinute: Int,
    val frequency: Frequency,
    val specificDaysCsv: String = "",
    val everyXHours: Int = 0,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val colorHex: String = "#0F9B8E",
    val icon: String = "💊",
    val missedAfterMinutes: Int = 60,
    val isActive: Boolean = true,
    val timesCsv: String = "",
    val quantityAvailable: Int? = null,
    val dosesPerIntake: Int = 1,
    /** Unused: reminder sound is now a single global setting (see ReminderSoundPrefs). Kept for schema compatibility. */
    val reminderSoundUri: String? = null
)

/** All configured dose times for the day, parsed from [Medicine.timesCsv] with a fallback to the single reminder time. */
fun Medicine.timesOfDay(): List<Pair<Int, Int>> {
    val parsed = timesCsv.split(",")
        .mapNotNull { entry ->
            val parts = entry.trim().split(":")
            if (parts.size != 2) return@mapNotNull null
            val h = parts[0].toIntOrNull() ?: return@mapNotNull null
            val m = parts[1].toIntOrNull() ?: return@mapNotNull null
            h to m
        }
    return if (parsed.isEmpty()) listOf(reminderHour to reminderMinute) else parsed.sortedWith(compareBy({ it.first }, { it.second }))
}

/** Estimated full days of supply left, or null if no stock is being tracked. */
fun Medicine.daysOfStockRemaining(dosesPerDay: Int): Int? {
    val qty = quantityAvailable ?: return null
    if (dosesPerDay <= 0) return null
    return qty / (dosesPerDay * dosesPerIntake)
}
