package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import com.aks.gallerysorter.core.Category

/**
 * User correction, never re-classified. isExcluded = true removes the item from [category];
 * false forces it into [category].
 */
@Entity(tableName = "overrides", primaryKeys = ["mediaId", "category"])
data class OverrideRow(val mediaId: Long, val category: Category, val isExcluded: Boolean)
