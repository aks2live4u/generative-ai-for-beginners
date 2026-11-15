package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aks.gallerysorter.index.entities.DupeRow

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

    @Transaction
    open suspend fun replaceAll(rows: List<DupeRow>) {
        clear()
        insertAll(rows)
    }
}
