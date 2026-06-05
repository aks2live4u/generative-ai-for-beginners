package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_vault")
data class ShoppingVaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String = "",
    val price: String = "",
    val notes: String = "",
    val sourceApp: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val reminderSentAt: Long = 0L,
    val isPurchased: Boolean = false,
    val isRemoved: Boolean = false
)
