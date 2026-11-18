package com.dosemate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.dosemate.data.ReminderSoundPrefs

class DoseMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val soundUri = Uri.parse(ReminderSoundPrefs.defaultSoundUri(this))
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val reminderChannel = NotificationChannel(
                CHANNEL_ID, "Medicine Reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take your medicines on time"
                enableVibration(true)
                setSound(soundUri, audioAttr)
            }
            // Info channel (low-stock, course ended) — no custom sound needed
            val infoChannel = NotificationChannel(
                INFO_CHANNEL_ID, "Medicine Alerts", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Low-stock and course completion alerts"
            }
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(infoChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "medicine_reminders_v2"
        const val INFO_CHANNEL_ID = "medicine_info"
    }
}
