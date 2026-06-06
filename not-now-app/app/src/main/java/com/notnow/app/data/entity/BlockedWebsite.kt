package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_websites")
data class BlockedWebsite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,           // e.g. "pornhub.com" — no scheme, no www
    val label: String,            // e.g. "PornHub"
    val frictionLevel: FrictionLevel = FrictionLevel.LEVEL_1_MINOR,
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
