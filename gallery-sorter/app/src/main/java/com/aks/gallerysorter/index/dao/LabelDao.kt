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

    @Query("SELECT * FROM labels WHERE mediaId = :mediaId")
    abstract suspend fun labelsFor(mediaId: Long): List<LabelRow>

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

    @Query(
        """
        SELECT m.* FROM media m
        WHERE (
            (EXISTS (SELECT 1 FROM labels l WHERE l.mediaId = m.id AND l.category = :category)
             AND NOT EXISTS (SELECT 1 FROM overrides o
                             WHERE o.mediaId = m.id AND o.category = :category AND o.isExcluded = 1))
            OR EXISTS (SELECT 1 FROM overrides o
                       WHERE o.mediaId = m.id AND o.category = :category AND o.isExcluded = 0)
        )
        AND (:includeLocked = 1 OR (
            NOT EXISTS (SELECT 1 FROM labels l
                        WHERE l.mediaId = m.id
                          AND l.category IN (:hidden)
                          AND NOT EXISTS (SELECT 1 FROM overrides o
                                          WHERE o.mediaId = l.mediaId AND o.category = l.category AND o.isExcluded = 1))
            AND NOT EXISTS (SELECT 1 FROM overrides o
                            WHERE o.mediaId = m.id AND o.isExcluded = 0 AND o.category IN (:hidden))
        ))
        ORDER BY m.dateTakenMs DESC, m.id DESC
        """
    )
    protected abstract fun itemsInQuery(
        category: Category,
        includeLocked: Int,
        hidden: List<String>,
    ): PagingSource<Int, MediaRow>

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
