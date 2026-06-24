package com.dosemate.scheduling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dosemate.DoseMateApp
import com.dosemate.MainActivity
import com.dosemate.R

object NotificationHelper {

    private fun channelIdFor(medicineId: Long, soundUri: String?): String {
        if (soundUri == null) return DoseMateApp.CHANNEL_ID
        return "medicine_reminders_${medicineId}_${soundUri.hashCode()}"
    }

    private fun ensureChannel(context: Context, channelId: String, soundUri: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || soundUri == null) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) != null) return
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(channelId, "Medicine Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders to take your medicines on time"
            enableVibration(true)
            setSound(Uri.parse(soundUri), audioAttributes)
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(context: Context, logId: Long, medicineId: Long, medicineName: String, dosage: String, soundUri: String? = null) {
        val channelId = channelIdFor(medicineId, soundUri)
        ensureChannel(context, channelId, soundUri)

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
                putExtra(EXTRA_SOUND_URI, soundUri)
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
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
    const val EXTRA_SOUND_URI = "extra_sound_uri"
}
