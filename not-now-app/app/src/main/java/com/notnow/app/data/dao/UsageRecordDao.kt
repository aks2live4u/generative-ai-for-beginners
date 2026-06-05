package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.InteractionType
import com.notnow.app.data.entity.UsageRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {
    @Insert
    suspend fun insert(record: UsageRecord)

    @Query("SELECT * FROM usage_records WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getRecordsSince(since: Long): Flow<List<UsageRecord>>

    @Query("SELECT COUNT(*) FROM usage_records WHERE timestamp >= :since AND interactionType = :type")
    suspend fun countByTypeSince(since: Long, type: InteractionType): Int

    @Query("""
        SELECT packageName, COUNT(*) as cnt
        FROM usage_records
        WHERE timestamp >= :since
        GROUP BY packageName
        ORDER BY cnt DESC
        LIMIT 1
    """)
    suspend fun getMostAttemptedPackageSince(since: Long): String?

    @Query("""
        SELECT strftime('%H', datetime(timestamp / 1000, 'unixepoch', 'localtime')) as hour, COUNT(*) as cnt
        FROM usage_records
        WHERE timestamp >= :since
        GROUP BY hour
        ORDER BY cnt DESC
        LIMIT 1
    """)
    suspend fun getPeakTriggerHourSince(since: Long): String?

    @Query("DELETE FROM usage_records WHERE timestamp < :before")
    suspend fun pruneOlderThan(before: Long)
}
