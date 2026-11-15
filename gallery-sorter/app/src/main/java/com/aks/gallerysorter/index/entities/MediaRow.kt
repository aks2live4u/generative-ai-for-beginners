package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.ScanStatus

/** One indexed media file. [id] is the MediaStore id. Faces live in [FaceRow]. */
@Entity(tableName = "media", indices = [Index("sha256"), Index("phash"), Index("status")])
data class MediaRow(
    @PrimaryKey val id: Long,
    val path: String,
    val kind: MediaKind,
    val sizeBytes: Long,
    val mime: String,
    val width: Int,
    val height: Int,
    val dateTakenMs: Long?,
    val durationMs: Long?,
    val generation: Long,
    val status: ScanStatus = ScanStatus.PENDING,
    val sha256: String? = null,
    val phash: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val city: String? = null,
)
