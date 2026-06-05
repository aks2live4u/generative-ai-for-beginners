package com.notnow.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notnow.app.MainActivity
import com.notnow.app.NotNowApplication
import com.notnow.app.R

class ShoppingReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NotNowApplication
        val pending = app.shoppingVaultRepository.getPendingReminders()

        pending.forEach { item ->
            sendReminder(item.id.toInt(), item.title)
            app.shoppingVaultRepository.markReminderSent(item.id)
        }

        return Result.success()
    }

    private fun sendReminder(id: Int, title: String) {
        val intent = PendingIntent.getActivity(
            applicationContext, id,
            Intent(applicationContext, MainActivity::class.java).apply {
                putExtra("open_vault", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, "reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Still want it?")
            .setContentText("You saved \"$title\" yesterday. Do you still want it?")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
