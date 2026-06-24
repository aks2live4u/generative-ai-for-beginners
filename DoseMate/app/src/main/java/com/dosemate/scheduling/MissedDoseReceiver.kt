package com.dosemate.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.LogStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MissedDoseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra(AlarmScheduler.EXTRA_LOG_ID, -1L)
        if (logId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DoseMateRepository(context)
                val log = repository.getLog(logId)
                if (log != null && log.status == LogStatus.PENDING) {
                    repository.updateLog(log.copy(status = LogStatus.MISSED))
                    NotificationHelper.cancel(context, logId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
