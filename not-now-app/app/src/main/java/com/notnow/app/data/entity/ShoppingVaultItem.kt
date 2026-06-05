package com.notnow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_vault")
data class ShoppingVaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String = "",
    val price: String = "",
    val note: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val reminderSentAt: Long = 0L,
    val isPurchased: Boolean = false
)
