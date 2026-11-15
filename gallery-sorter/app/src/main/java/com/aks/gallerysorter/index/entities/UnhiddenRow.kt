package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Content the user took out of the vault ([plainSha256], lowercase hex SHA-256 of the restored
 * bytes). Auto-hide never re-hides it; an explicit hide removes the row. [restoredPath] is where
 * the unhide put it, [atMs] when.
 */
@Entity(tableName = "user_unhidden")
class UnhiddenRow(
    @PrimaryKey val plainSha256: String,
    val restoredPath: String,
    val atMs: Long,
)
