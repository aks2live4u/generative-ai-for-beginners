package com.aichiefofstaff.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#6D28D9",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
