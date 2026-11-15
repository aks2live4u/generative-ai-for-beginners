package com.aks.gallerysorter.scan

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aks.gallerysorter.graph
import java.util.concurrent.TimeUnit

/**
 * Background trigger for scans: periodic every 6 h, plus debounced one-offs from [MediaObserver]. It hands
 * the scan to [ScanService]; where Android forbids starting a foreground service from the background, it
 * scans inline instead (incremental scans are short, and an interrupted scan resumes next time).
 */
class ScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            ScanService.start(applicationContext)
            return Result.success()
        } catch (e: IllegalStateException) { // ForegroundServiceStartNotAllowedException on API 31+
            Log.i(TAG, "foreground scan not allowed from the background; scanning in the worker")
        }
        return try {
            ScanService.runScan(applicationContext) { _, _ -> }
            Result.success()
        } catch (e: ScanAbortedException) {
            Log.w(TAG, "scan aborted after repeated failures; retrying later")
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC = "scan-periodic"
        private const val ON_CHANGE = "scan-on-change"

        /** (Re)schedules the 6-hourly scan with the current charging requirement. */
        suspend fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScanWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints(context))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** One-off scan after media changes; a scan already queued is kept. */
        suspend fun enqueueOnChange(context: Context) {
            val request = OneTimeWorkRequestBuilder<ScanWorker>().setConstraints(constraints(context)).build()
            WorkManager.getInstance(context).enqueueUniqueWork(ON_CHANGE, ExistingWorkPolicy.KEEP, request)
        }

        private suspend fun constraints(context: Context): Constraints {
            return Constraints.Builder().setRequiresCharging(context.graph.scanSettings.requiresCharging()).build()
        }
    }
}
