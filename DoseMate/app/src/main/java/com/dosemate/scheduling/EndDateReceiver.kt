package com.dosemate.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosemate.data.DoseMateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EndDateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        if (medicineId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicine = DoseMateRepository(context).getMedicine(medicineId) ?: return@launch
                NotificationHelper.showCourseEnded(context, medicineId, medicine.name)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
