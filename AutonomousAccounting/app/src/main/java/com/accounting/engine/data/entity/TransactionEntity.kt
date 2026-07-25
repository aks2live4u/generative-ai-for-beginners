package com.accounting.engine.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val rawInput: String,
    val amount: Double,
    val timestamp: Long,
    val source: TransactionSource
)
