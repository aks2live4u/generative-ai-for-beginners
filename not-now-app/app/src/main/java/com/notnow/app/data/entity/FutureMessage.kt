package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "future_messages")
data class FutureMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
