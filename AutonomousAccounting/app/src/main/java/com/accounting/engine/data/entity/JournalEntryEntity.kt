package com.accounting.engine.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["transactionId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val timestamp: Long,
    val description: String
)
