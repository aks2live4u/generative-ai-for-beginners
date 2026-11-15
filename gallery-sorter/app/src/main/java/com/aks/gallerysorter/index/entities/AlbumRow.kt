package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user album: a virtual grouping, nothing is copied (spec 6.4). */
@Entity(tableName = "albums")
data class AlbumRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(
    tableName = "album_items",
    primaryKeys = ["albumId", "mediaId"],
    foreignKeys = [
        ForeignKey(AlbumRow::class, ["id"], ["albumId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MediaRow::class, ["id"], ["mediaId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("mediaId")],
)
data class AlbumItemRow(val albumId: Long, val mediaId: Long)
