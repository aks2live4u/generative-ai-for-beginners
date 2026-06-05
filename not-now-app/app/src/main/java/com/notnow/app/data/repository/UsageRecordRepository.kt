package com.notnow.app.data.repository

import com.notnow.app.data.dao.UsageRecordDao
import com.notnow.app.data.entity.InteractionType
import com.notnow.app.data.entity.UsageRecord
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class UsageRecordRepository(private val dao: UsageRecordDao) {

    suspend fun record(packageName: String, appName: String, type: InteractionType, delaySeconds: Int = 0) {
        dao.insert(UsageRecord(packageName = packageName, appName = appName, interactionType = type, delayApplied = delaySeconds))
    }

    fun getWeeklyRecords(): Flow<List<UsageRecord>> {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return dao.getRecordsSince(weekAgo)
    }

    suspend fun countEmergencyUnlocksThisWeek(): Int {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return dao.countByTypeSince(weekAgo, InteractionType.BYPASSED)
    }

    suspend fun getMostAttemptedApp(): String? {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return dao.getMostAttemptedPackageSince(weekAgo)
    }

    suspend fun getPeakTriggerHour(): String? {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return dao.getPeakTriggerHourSince(weekAgo)
    }

    suspend fun pruneOldRecords() {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        dao.pruneOlderThan(thirtyDaysAgo)
    }
}
