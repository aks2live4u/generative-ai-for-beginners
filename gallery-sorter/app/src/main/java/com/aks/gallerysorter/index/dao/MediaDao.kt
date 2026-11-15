package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.index.entities.MediaRow
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Upsert suspend fun upsert(row: MediaRow)
    @Upsert suspend fun upsertAll(rows: List<MediaRow>)

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun get(id: Long): MediaRow?

    @Query("SELECT * FROM media WHERE status = :status ORDER BY id LIMIT :limit")
    suspend fun byStatus(status: ScanStatus, limit: Int): List<MediaRow>

    @Query("UPDATE media SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: ScanStatus)

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT id FROM media")
    suspend fun allIds(): List<Long>

    @Query("SELECT COUNT(*) FROM media")
    fun count(): Flow<Int>
}
