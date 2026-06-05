package com.notnow.app.data.repository

import com.notnow.app.data.dao.UsageRecordDao
import com.notnow.app.data.entity.AccessOutcome
import com.notnow.app.data.entity.UsageRecord
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class UsageRepository(private val dao: UsageRecordDao) {

    fun getRecordsSince(ms: Long): Flow<List<UsageRecord>> = dao.observeSince(ms)

    suspend fun record(packageName: String, appName: String, outcome: AccessOutcome, delaySec: Long = 0) {
        dao.insert(UsageRecord(packageName = packageName, appName = appName, outcome = outcome, delaySeconds = delaySec))
    }

    suspend fun getWeeklyStats(): WeeklyStats {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val mostAttempted = dao.getMostAttemptedSince(weekAgo)
        val emergencyCount = dao.countEmergencyUnlocks(weekAgo)
        return WeeklyStats(
            mostAttemptedApp = mostAttempted?.appName ?: "—",
            emergencyUnlocks = emergencyCount
        )
    }

    suspend fun pruneOldRecords() {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        dao.deleteOlderThan(thirtyDaysAgo)
    }
}

data class WeeklyStats(
    val mostAttemptedApp: String,
    val emergencyUnlocks: Int
)
