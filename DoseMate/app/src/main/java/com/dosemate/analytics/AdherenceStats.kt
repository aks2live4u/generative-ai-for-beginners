package com.dosemate.analytics

data class AdherenceStats(
    val adherencePercent: Int,
    val averageDelayMinutes: Int,
    val currentStreakDays: Int,
    val missedCount: Int,
    val takenCount: Int,
    val totalCount: Int
)

data class TrendPoint(val label: String, val value: Float)

data class MedicineStats(
    val medicineName: String,
    val averageDelayMinutes: Int,
    val missedCount: Int,
    val consistencyScore: Int,
    val usualTakenTimeLabel: String?
)
