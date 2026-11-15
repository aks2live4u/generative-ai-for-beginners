package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier

/**
 * One hidden file. [categories] is a comma-joined list of Category names.
 * [vaultFile] and [thumbFile] are relative to the tier's vault root (`files/<uuid>.enc`,
 * `thumbs/<uuid>.thumb.enc`). The thumbnail has its own per-file key, [thumbWrappedKey].
 * [plainSha256] is the lowercase hex SHA-256 of the original bytes, checked on unhide and move.
 */
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
    val thumbWrappedKey: ByteArray?,
    val plainSha256: String,
)
