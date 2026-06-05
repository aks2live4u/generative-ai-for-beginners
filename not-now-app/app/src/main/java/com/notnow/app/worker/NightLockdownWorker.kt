package com.notnow.app.worker

import android.content.Context
import androidx.work.*
import com.notnow.app.data.preferences.AppPreferences
import java.util.*
import java.util.concurrent.TimeUnit

class NightLockdownActivateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        AppPreferences.getInstance(applicationContext).setNightLockdownActive(true)
        NightLockdownScheduler.scheduleDeactivation(applicationContext)
        return Result.success()
    }
}

class NightLockdownDeactivateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        AppPreferences.getInstance(applicationContext).setNightLockdownActive(false)
        NightLockdownScheduler.scheduleActivation(applicationContext)
        return Result.success()
    }
}

object NightLockdownScheduler {

    private const val TAG_ACTIVATE = "night_lockdown_activate"
    private const val TAG_DEACTIVATE = "night_lockdown_deactivate"

    fun scheduleActivation(context: Context, startHour: Int = 23) {
        val delay = delayUntilHour(startHour)
        val request = OneTimeWorkRequestBuilder<NightLockdownActivateWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(TAG_ACTIVATE)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TAG_ACTIVATE,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleDeactivation(context: Context, endHour: Int = 7) {
        val delay = delayUntilHour(endHour)
        val request = OneTimeWorkRequestBuilder<NightLockdownDeactivateWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(TAG_DEACTIVATE)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TAG_DEACTIVATE,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_ACTIVATE)
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_DEACTIVATE)
    }

    private fun delayUntilHour(targetHour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
