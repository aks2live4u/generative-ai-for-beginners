package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.aks.gallerysorter.index.entities.DupeRow
import com.aks.gallerysorter.index.entities.MediaRow

/** One member of a duplicate group, with its media row. */
data class DupeItem(val groupId: Long, val suggestedKeep: Boolean, @Embedded val media: MediaRow)

@Dao
abstract class DupeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(rows: List<DupeRow>)

    @Query("DELETE FROM dupes")
    abstract suspend fun clear()

    @Query("SELECT * FROM dupes ORDER BY groupId, mediaId")
    abstract suspend fun all(): List<DupeRow>

    @Query("SELECT * FROM dupes WHERE groupId = :groupId")
    abstract suspend fun group(groupId: Long): List<DupeRow>

    @Query("SELECT COUNT(DISTINCT groupId) FROM dupes")
    abstract fun groupCount(): Flow<Int>

    /**
     * Duplicate groups with their media, by group. While locked, a group with any member that
     * the lock filter hides (Sensitive/Documents/Review) is left out entirely.
     */
    fun visibleGroups(includeLocked: Boolean): Flow<List<DupeItem>> = visibleGroupsQuery(includeLocked.bit(), HIDDEN_NAMES)

    @Query(
        "SELECT d.groupId, d.suggestedKeep, m.* FROM dupes d JOIN media m ON m.id = d.mediaId " +
            "WHERE NOT EXISTS (SELECT 1 FROM dupes d2 JOIN media m ON m.id = d2.mediaId " +
            "WHERE d2.groupId = d.groupId AND NOT $LOCK_FILTER) " +
            "ORDER BY d.groupId, d.suggestedKeep DESC, m.id",
    )
    protected abstract fun visibleGroupsQuery(includeLocked: Int, hidden: List<String>): Flow<List<DupeItem>>

    @Transaction
    open suspend fun replaceAll(rows: List<DupeRow>) {
        clear()
        insertAll(rows)
    }
}
