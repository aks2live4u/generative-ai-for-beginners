package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FrictionLevel(val delaySeconds: Int, val label: String) {
    LEVEL_1_DISTRACTION(30, "30 sec"),
    LEVEL_2_ATTENTION_TRAP(600, "10 min"),
    LEVEL_3_SPENDING(3600, "60 min"),
    LEVEL_4_BLOCKED(0, "Blocked")
}

enum class AppCategory {
    SOCIAL_MEDIA, SHOPPING, ENTERTAINMENT, NEWS, ADULT, COMMUNICATION, PRODUCTIVITY, OTHER
}

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: AppCategory,
    val frictionLevel: FrictionLevel,
    val isEnabled: Boolean = true,
    val blockedDuringFocusMode: Boolean = true,
    val blockedDuringNightLockdown: Boolean = true
)
