package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccessOutcome { WAITED, WENT_BACK, EMERGENCY_UNLOCKED, NIGHT_BLOCKED }

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val attemptedAt: Long = System.currentTimeMillis(),
    val outcome: AccessOutcome,
    val delaySeconds: Long = 0
)
