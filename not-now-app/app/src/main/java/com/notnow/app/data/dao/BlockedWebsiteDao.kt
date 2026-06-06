package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.BlockedWebsite
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedWebsiteDao {
    @Query("SELECT * FROM blocked_websites ORDER BY label ASC")
    fun observeAll(): Flow<List<BlockedWebsite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: BlockedWebsite): Long

    @Delete
    suspend fun delete(site: BlockedWebsite)

    @Query("UPDATE blocked_websites SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
