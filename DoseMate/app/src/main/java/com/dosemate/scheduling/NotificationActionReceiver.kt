package com.dosemate.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.LogStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val SNOOZE_MINUTES = 10L

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra(AlarmScheduler.EXTRA_LOG_ID, -1L)
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: ""
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        val soundUri = intent.getStringExtra(NotificationHelper.EXTRA_SOUND_URI)
        if (logId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DoseMateRepository(context)
                when (intent.action) {
                    NotificationHelper.ACTION_TAKEN -> {
                        val log = repository.getLog(logId) ?: return@launch
                        val now = System.currentTimeMillis()
                        val delayMinutes = ((now - log.scheduledEpochMillis) / 60_000L).toInt()
                        repository.updateLog(
                            log.copy(
                                status = LogStatus.TAKEN,
                                actualTakenEpochMillis = now,
                                delayMinutes = delayMinutes
                            )
                        )
                        NotificationHelper.cancel(context, logId)
                    }

                    NotificationHelper.ACTION_SKIP -> {
                        val log = repository.getLog(logId) ?: return@launch
                        repository.updateLog(log.copy(status = LogStatus.SKIPPED))
                        NotificationHelper.cancel(context, logId)
                    }

                    NotificationHelper.ACTION_SNOOZE -> {
                        NotificationHelper.cancel(context, logId)
                        scheduleSnoozeReshow(context, logId, medicineId, medicineName, dosage, soundUri)
                    }

                    ACTION_SHOW_SNOOZED -> {
                        val log = repository.getLog(logId)
                        if (log != null && log.status == LogStatus.PENDING) {
                            NotificationHelper.showReminder(context, logId, medicineId, medicineName, dosage, soundUri)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun scheduleSnoozeReshow(context: Context, logId: Long, medicineId: Long, medicineName: String, dosage: String, soundUri: String?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SHOW_SNOOZED
            putExtra(AlarmScheduler.EXTRA_LOG_ID, logId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(NotificationHelper.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_SOUND_URI, soundUri)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, (4_000_000 + logId).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    companion object {
        const val ACTION_SHOW_SNOOZED = "com.dosemate.action.SHOW_SNOOZED"
    }
}
