package com.notnow.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.notnow.app.data.database.AppDatabase
import com.notnow.app.data.repository.ShoppingVaultRepository
import java.util.concurrent.TimeUnit

class ShoppingReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        private const val CHANNEL_ID = "shopping_reminders"
        private const val TAG = "shopping_reminder"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ShoppingReminderWorker>(6, TimeUnit.HOURS)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repo = ShoppingVaultRepository(db.shoppingVaultDao())
        val items = repo.getItemsDueForReminder()

        if (items.isEmpty()) return Result.success()

        ensureChannel()

        items.take(3).forEach { item ->
            sendNotification(item.id.toInt(), item.title)
            repo.markReminderSent(item.id)
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Still want this?")
            .setContentText("You saved \"$title\" yesterday. Do you still want it?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }

    private fun ensureChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Shopping Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
