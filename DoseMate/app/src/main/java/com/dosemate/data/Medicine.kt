package com.dosemate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * specificDaysCsv: comma-separated java.time.DayOfWeek ordinal values, only used when
 * frequency == SPECIFIC_DAYS. everyXHours is only used when frequency == EVERY_X_HOURS.
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
    val isActive: Boolean = true
)
