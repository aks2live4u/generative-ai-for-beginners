package com.dosemate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_logs")
data class MedicineLog(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val scheduledEpochMillis: Long,
    val actualTakenEpochMillis: Long? = null,
    val status: LogStatus = LogStatus.PENDING,
    val delayMinutes: Int? = null,
    val dateEpochDay: Long
)
