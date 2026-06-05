package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.UsageRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {
    @Query("SELECT * FROM usage_records WHERE attemptedAt >= :sinceMs ORDER BY attemptedAt DESC")
    fun observeSince(sinceMs: Long): Flow<List<UsageRecord>>

    @Query("""
        SELECT packageName, appName, COUNT(*) as cnt 
        FROM usage_records 
        WHERE attemptedAt >= :sinceMs
        GROUP BY packageName 
        ORDER BY cnt DESC 
        LIMIT 1
    """)
    suspend fun getMostAttemptedSince(sinceMs: Long): MostAttemptedResult?

    @Query("SELECT COUNT(*) FROM usage_records WHERE attemptedAt >= :sinceMs AND outcome = 'EMERGENCY_UNLOCKED'")
    suspend fun countEmergencyUnlocks(sinceMs: Long): Int

    @Insert
    suspend fun insert(record: UsageRecord)

    @Query("DELETE FROM usage_records WHERE attemptedAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}

data class MostAttemptedResult(val packageName: String, val appName: String, val cnt: Int)
