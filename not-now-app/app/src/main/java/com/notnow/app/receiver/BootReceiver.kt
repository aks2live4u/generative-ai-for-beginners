package com.notnow.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.notnow.app.worker.NightLockdownWorker
import com.notnow.app.worker.ShoppingReminderWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scheduleWorkers(context)
        }
    }

    companion object {
        fun scheduleWorkers(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Shopping reminder: check every 6 hours
            workManager.enqueueUniquePeriodicWork(
                "shopping_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ShoppingReminderWorker>(6, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build())
                    .build()
            )

            // Night lockdown check: every 30 minutes
            workManager.enqueueUniquePeriodicWork(
                "night_lockdown_check",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NightLockdownWorker>(30, TimeUnit.MINUTES)
                    .build()
            )
        }
    }
}
