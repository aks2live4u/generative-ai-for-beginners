package com.focus.launcher

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** "Your 10 minutes on Instagram are up" — a nudge to close a distracting app on time. */
object SessionReminder {
    private const val CHANNEL = "session_reminders"
    private const val EXTRA_LABEL = "label"
    private const val EXTRA_MINUTES = "minutes"

    fun schedule(context: Context, app: AppEntry, minutes: Int) {
        if (minutes <= 0) return
        val intent = Intent(context, SessionReminderReceiver::class.java)
            .putExtra(EXTRA_LABEL, app.label)
            .putExtra(EXTRA_MINUTES, minutes)
        val pending = PendingIntent.getBroadcast(
            context, app.key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pending,
            )
        }
    }

    internal fun show(context: Context, intent: Intent) {
        // Already back on the home screen: the session ended by itself, no nudge needed.
        if (MainActivity.inForeground) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val label = intent.getStringExtra(EXTRA_LABEL) ?: return
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Session reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "A nudge when the time you chose for a distracting app is up"
            }
        )
        val home = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("$minutes minutes on $label")
            .setContentText("That was the plan. Tap to go home.")
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(home)
            .setAutoCancel(true)
            .build()
        manager.notify(label.hashCode(), notification)
    }
}

class SessionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = SessionReminder.show(context, intent)
}
