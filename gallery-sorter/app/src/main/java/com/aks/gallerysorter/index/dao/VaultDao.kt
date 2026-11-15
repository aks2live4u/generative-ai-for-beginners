package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.entities.VaultRow
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Insert suspend fun insert(row: VaultRow): Long

    @Query("SELECT * FROM vault WHERE id = :id")
    suspend fun get(id: Long): VaultRow?

    @Query("SELECT * FROM vault WHERE tier = :tier ORDER BY id DESC")
    fun byTier(tier: VaultTier): Flow<List<VaultRow>>

    @Query("SELECT * FROM vault ORDER BY id DESC")
    fun all(): Flow<List<VaultRow>>

    @Query("DELETE FROM vault WHERE id = :id")
    suspend fun delete(id: Long)
}
