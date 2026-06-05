package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FrictionLevel(val delaySeconds: Long) {
    LEVEL_1_MINOR(30),
    LEVEL_2_ATTENTION(600),
    LEVEL_3_SPENDING(3600),
    LEVEL_4_BLOCKED(Long.MAX_VALUE)
}

enum class AppCategory {
    SOCIAL, SHOPPING, ENTERTAINMENT, NEWS, ADULT, OTHER
}

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: AppCategory,
    val frictionLevel: FrictionLevel,
    val blockedInFocusMode: Boolean = true,
    val blockedAtNight: Boolean = true,
    val isEnabled: Boolean = true
)
