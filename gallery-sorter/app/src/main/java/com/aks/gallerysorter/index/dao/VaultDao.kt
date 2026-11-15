package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.entities.VaultRow
import kotlinx.coroutines.flow.Flow

/** The files one vault row owns, relative to its tier's root. */
data class VaultFileRef(val tier: VaultTier, val vaultFile: String, val thumbFile: String?)

@Dao
interface VaultDao {
    @Insert suspend fun insert(row: VaultRow): Long

    @Query("SELECT * FROM vault WHERE id = :id")
    suspend fun get(id: Long): VaultRow?

    @Query("SELECT * FROM vault WHERE tier = :tier ORDER BY id DESC")
    fun byTier(tier: VaultTier): Flow<List<VaultRow>>

    @Query("SELECT * FROM vault ORDER BY id DESC")
    fun all(): Flow<List<VaultRow>>

    @Query("SELECT tier, vaultFile, thumbFile FROM vault")
    suspend fun fileRefs(): List<VaultFileRef>

    @Query("SELECT * FROM vault WHERE originalPath = :path")
    suspend fun byOriginalPath(path: String): List<VaultRow>

    @Query("SELECT * FROM vault WHERE tier = :tier AND verified = 0 ORDER BY id")
    suspend fun unverified(tier: VaultTier): List<VaultRow>

    @Query("UPDATE vault SET verified = 1 WHERE id = :id")
    suspend fun setVerified(id: Long)

    @Query("DELETE FROM vault WHERE id = :id")
    suspend fun delete(id: Long)
}
