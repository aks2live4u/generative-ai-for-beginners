package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("TASK_ID", 0L)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Timeline Task"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Create Notification Channel if on Android 8.0+
        val channelId = "structured_plus_alarms"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Agenda Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gentle prompts for scheduled items on Structured+"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Create entry launch intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = if (launchIntent != null) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            PendingIntent.getActivity(context, taskId.toInt(), launchIntent, flags)
        } else {
            null
        }

        val startTime = intent.getStringExtra("TASK_START_TIME")
        val reminderMinutes = intent.getIntExtra("TASK_REMINDER_MINUTES", 15)
        val contentText = if (!startTime.isNullOrEmpty()) {
            if (reminderMinutes > 0) {
                "\"$taskTitle\" starts in $reminderMinutes min — at $startTime."
            } else {
                "\"$taskTitle\" is starting right now!"
            }
        } else {
            "Upcoming: \"$taskTitle\""
        }

        // 3. Build & Dispatch the custom notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏳ Upcoming Event!")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(taskId.toInt(), builder.build())
    }
}
