package com.accounting.engine.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.accounting.engine.AccountingApplication
import kotlinx.coroutines.flow.first

/**
 * Phase 4 background automation: on a monthly cadence, sets aside the user-configured
 * percentage (default 10%, see [com.accounting.engine.data.UserPreferences]) of accumulated
 * Retained Earnings into the Contingency Reserve - the same "Set aside 10% for contingency"
 * journal entry a user could trigger manually, just run unattended.
 */
class ContingencyReserveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as AccountingApplication
        val percentage = app.userPreferences.contingencyReservePercentage.first()

        return try {
            app.repository.runContingencyAllocation(percentage)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "contingency_reserve_auto_allocation"
    }
}
