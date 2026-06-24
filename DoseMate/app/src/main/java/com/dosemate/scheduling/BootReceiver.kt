package com.dosemate.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosemate.data.DoseMateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DoseMateRepository(context)
                repository.getActiveMedicines().forEach { medicine ->
                    AlarmScheduler.scheduleNext(context, medicine)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
