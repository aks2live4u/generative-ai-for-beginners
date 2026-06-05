package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InteractionType {
    BLOCKED,          // countdown shown, user waited
    BYPASSED,         // emergency unlock used
    SHOPPING_SAVED,   // item saved to vault
    SHOPPING_BOUGHT,  // bought after countdown
    ABANDONED         // left before countdown finished
}

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val interactionType: InteractionType,
    val delayApplied: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
