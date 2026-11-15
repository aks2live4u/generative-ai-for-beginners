package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.aks.gallerysorter.core.Category

@Entity(
    tableName = "labels",
    primaryKeys = ["mediaId", "category"],
    foreignKeys = [ForeignKey(MediaRow::class, ["id"], ["mediaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("category")],
)
data class LabelRow(val mediaId: Long, val category: Category, val confidence: Float)
