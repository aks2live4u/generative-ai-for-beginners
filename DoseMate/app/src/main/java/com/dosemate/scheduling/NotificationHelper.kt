package com.dosemate.scheduling

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dosemate.DoseMateApp
import com.dosemate.MainActivity
import com.dosemate.R

object NotificationHelper {

    fun showReminder(context: Context, logId: Long, medicineId: Long, medicineName: String, dosage: String) {
        val contentIntent = PendingIntent.getActivity(
            context, medicineId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmScheduler.EXTRA_LOG_ID, logId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSAGE, dosage)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(AlarmScheduler.EXTRA_LOG_ID, logId)
                putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSAGE, dosage)
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, DoseMateApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to take $medicineName")
            .setContentText(dosage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .addAction(0, "✓ Taken", actionIntent(ACTION_TAKEN, logId.toInt()))
            .addAction(0, "Snooze 10 min", actionIntent(ACTION_SNOOZE, (2_000_000 + logId).toInt()))
            .addAction(0, "Skip", actionIntent(ACTION_SKIP, (3_000_000 + logId).toInt()))
            .build()

        NotificationManagerCompat.from(context).notify(logId.toInt(), notification)
    }

    fun cancel(context: Context, logId: Long) {
        NotificationManagerCompat.from(context).cancel(logId.toInt())
    }

    fun showLowStock(context: Context, medicineId: Long, medicineName: String, daysLeft: Int) {
        val contentIntent = PendingIntent.getActivity(
            context, medicineId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val daysText = if (daysLeft <= 0) "today" else "in about $daysLeft day${if (daysLeft == 1) "" else "s"}"
        val notification = NotificationCompat.Builder(context, DoseMateApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$medicineName is running low")
            .setContentText("You'll run out $daysText. Time to restock.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        NotificationManagerCompat.from(context).notify((LOW_STOCK_NOTIFICATION_OFFSET + medicineId).toInt(), notification)
    }

    private const val LOW_STOCK_NOTIFICATION_OFFSET = 5_000_000

    const val ACTION_TAKEN = "com.dosemate.action.TAKEN"
    const val ACTION_SNOOZE = "com.dosemate.action.SNOOZE"
    const val ACTION_SKIP = "com.dosemate.action.SKIP"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSAGE = "extra_dosage"
}
