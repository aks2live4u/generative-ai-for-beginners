package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.Repository
import com.example.data.TimelineTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val db = AppDatabase.getDatabase(context)
        val repository = Repository(db)

        CoroutineScope(Dispatchers.IO).launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val allTasks = repository.getAllTasks().first()
            allTasks.filter { it.hasReminder && it.dayDate >= todayStr }
                .forEach { scheduleReminder(context, it) }
        }
    }

    private fun scheduleReminder(context: Context, task: TimelineTask) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val baseDate = sdf.parse(task.dayDate) ?: return
            val calendar = Calendar.getInstance().apply {
                time = baseDate
                val parts = task.timeSlotStart.split(":")
                set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9)
                set(Calendar.MINUTE, parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val eventTime = calendar.timeInMillis
            calendar.add(Calendar.MINUTE, -task.reminderMinutesBefore)
            var triggerTime = calendar.timeInMillis
            val now = System.currentTimeMillis()
            if (triggerTime <= now && eventTime > now) triggerTime = now + 5000
            if (triggerTime <= now) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = PendingIntent.getBroadcast(
                context, task.id,
                Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("TASK_ID", task.id.toLong())
                    putExtra("TASK_TITLE", task.title)
                    putExtra("TASK_START_TIME", task.timeSlotStart)
                    putExtra("TASK_REMINDER_MINUTES", task.reminderMinutesBefore)
                },
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            )

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() ->
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, pi), pi)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, pi), pi)
                else -> alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
