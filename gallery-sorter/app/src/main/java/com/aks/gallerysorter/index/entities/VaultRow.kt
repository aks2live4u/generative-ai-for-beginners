package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier

/** [categories] is a comma-joined list of Category names. */
@Entity(tableName = "vault")
class VaultRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val tier: VaultTier,
    val vaultFile: String,
    val kind: MediaKind,
    val sizeBytes: Long,
    val categories: String,
    val wrappedFileKey: ByteArray,
    val thumbFile: String?,
)
