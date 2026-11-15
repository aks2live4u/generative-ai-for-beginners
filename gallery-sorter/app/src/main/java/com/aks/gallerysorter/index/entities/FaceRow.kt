package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "faces",
    foreignKeys = [ForeignKey(MediaRow::class, ["id"], ["mediaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("mediaId"), Index("personId")],
)
class FaceRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val personId: Long?,
    val embedding: ByteArray,
)
