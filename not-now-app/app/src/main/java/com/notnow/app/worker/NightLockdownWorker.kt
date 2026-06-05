package com.notnow.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notnow.app.NotNowApplication
import com.notnow.app.data.repository.UsageRepository
import kotlinx.coroutines.flow.first

class NightLockdownWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NotNowApplication
        // Prune old usage records to keep the DB small
        app.usageRepository.pruneOldRecords()
        return Result.success()
    }
}
