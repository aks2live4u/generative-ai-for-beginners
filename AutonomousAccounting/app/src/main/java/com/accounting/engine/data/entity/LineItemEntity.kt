package com.accounting.engine.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "line_items",
    indices = [Index("journalEntryId"), Index("accountId")],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class LineItemEntity(
    @PrimaryKey val id: String,
    val journalEntryId: String,
    val accountId: String,
    val type: LineType,
    val amount: Double
)
