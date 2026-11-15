package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aks.gallerysorter.index.entities.UnhiddenRow

@Dao
interface UnhiddenDao {
    @Upsert suspend fun put(row: UnhiddenRow)

    @Query("SELECT EXISTS(SELECT 1 FROM user_unhidden WHERE plainSha256 = :sha256)")
    suspend fun contains(sha256: String): Boolean

    @Query("DELETE FROM user_unhidden WHERE plainSha256 = :sha256")
    suspend fun delete(sha256: String)
}
