package com.aichiefofstaff.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aichiefofstaff.data.db.InboxItemType

@Entity(tableName = "inbox_items")
data class InboxItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val type: InboxItemType = InboxItemType.TEXT,
    val sourceUri: String? = null,
    val processed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
