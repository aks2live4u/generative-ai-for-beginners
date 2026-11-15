package com.aks.gallerysorter.index.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.OverrideRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CategoryCount(val category: Category, val n: Int)

@Dao
abstract class LabelDao {
    @Upsert abstract suspend fun upsertAll(labels: List<LabelRow>)
    @Upsert abstract suspend fun upsertOverride(row: OverrideRow)

    @Query("DELETE FROM labels WHERE mediaId = :mediaId")
    abstract suspend fun clearLabels(mediaId: Long)

    @Query("DELETE FROM labels WHERE mediaId = :mediaId AND category = :category")
    abstract suspend fun deleteLabel(mediaId: Long, category: Category)

    @Query("SELECT * FROM labels WHERE mediaId = :mediaId")
    abstract suspend fun labelsFor(mediaId: Long): List<LabelRow>

    @Query("SELECT * FROM overrides WHERE mediaId = :mediaId")
    abstract suspend fun overridesFor(mediaId: Long): List<OverrideRow>

    /**
     * Items in [category], newest first. Honours user overrides (excluded = removed, not excluded = forced in).
     * When [includeLocked] is false, items carrying any label in SENSITIVE/DOCUMENTS/REVIEW are excluded
     * (band-confidence sensitive/doc items always also carry a REVIEW label from the categorizer).
     */
    fun itemsIn(category: Category, includeLocked: Boolean): PagingSource<Int, MediaRow> =
        itemsInQuery(
            category = category,
            includeLocked = if (includeLocked) 1 else 0,
            hidden = LOCKED_HIDDEN.map { it.name },
        )

    /** Up to [limit] ids of [itemsIn], newest first (Home tile previews). */
    suspend fun previewIds(category: Category, includeLocked: Boolean, limit: Int): List<Long> =
        previewIdsQuery(category, if (includeLocked) 1 else 0, LOCKED_HIDDEN.map { it.name }, limit)

    /** Every id of [itemsIn] ("Hide all (N)"). */
    suspend fun idsIn(category: Category, includeLocked: Boolean): List<Long> =
        previewIdsQuery(category, if (includeLocked) 1 else 0, LOCKED_HIDDEN.map { it.name }, -1)

    @Query("SELECT m.* FROM media m WHERE $IN_CATEGORY AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC")
    protected abstract fun itemsInQuery(
        category: Category,
        includeLocked: Int,
        hidden: List<String>,
    ): PagingSource<Int, MediaRow>

    @Query("SELECT m.id FROM media m WHERE $IN_CATEGORY AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC LIMIT :limit")
    protected abstract suspend fun previewIdsQuery(
        category: Category,
        includeLocked: Int,
        hidden: List<String>,
        limit: Int,
    ): List<Long>

    @Query(
        """
        SELECT category, COUNT(*) AS n FROM (
            SELECT mediaId, category FROM labels
            UNION
            SELECT mediaId, category FROM overrides WHERE isExcluded = 0
        ) lc
        WHERE NOT EXISTS (SELECT 1 FROM overrides o
                          WHERE o.mediaId = lc.mediaId AND o.category = lc.category AND o.isExcluded = 1)
        GROUP BY category
        """
    )
    protected abstract fun rawCounts(): Flow<List<CategoryCount>>

    fun counts(): Flow<Map<Category, Int>> =
        rawCounts().map { rows -> rows.associate { it.category to it.n } }

    companion object {
        /** Categories whose items are hidden while locked (the REVIEW label marks review-band items). */
        val LOCKED_HIDDEN: List<Category> = Category.lockedExcluded + Category.REVIEW
    }
}
