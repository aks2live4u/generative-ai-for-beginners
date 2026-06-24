package com.dosemate.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.LogStatus
import com.dosemate.data.MedicineLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val slotIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT_INDEX, 0)
        if (medicineId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DoseMateRepository(context)
                val medicine = repository.getMedicine(medicineId)
                if (medicine != null && medicine.isActive) {
                    val now = System.currentTimeMillis()
                    val log = MedicineLog(
                        medicineId = medicine.medicineId,
                        medicineName = medicine.name,
                        scheduledEpochMillis = now,
                        status = LogStatus.PENDING,
                        dateEpochDay = LocalDate.now().toEpochDay()
                    )
                    val logId = repository.insertLog(log)

                    NotificationHelper.showReminder(context, logId, medicine.medicineId, medicine.name, medicine.dosage, medicine.reminderSoundUri)
                    AlarmScheduler.scheduleMissedCheck(context, logId, medicine.medicineId, now, medicine.missedAfterMinutes)
                    AlarmScheduler.scheduleNextForSlot(context, medicine, slotIndex)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
