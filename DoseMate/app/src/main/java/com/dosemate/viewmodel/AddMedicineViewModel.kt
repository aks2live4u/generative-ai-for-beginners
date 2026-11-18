package com.dosemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.Frequency
import com.dosemate.data.Medicine
import com.dosemate.scheduling.AlarmScheduler
import kotlinx.coroutines.launch

class AddMedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoseMateRepository(application)

    suspend fun loadMedicine(medicineId: Long): Medicine? = repository.getMedicine(medicineId)

    fun saveMedicine(
        editingMedicineId: Long?,
        name: String,
        dosage: String,
        times: List<Pair<Int, Int>>,
        frequency: Frequency,
        specificDays: Set<Int>,
        everyXHours: Int,
        startDateEpochDay: Long,
        endDateEpochDay: Long?,
        colorHex: String,
        icon: String,
        missedAfterMinutes: Int,
        quantityAvailable: Int?,
        dosesPerIntake: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val orderedTimes = times.sortedWith(compareBy({ it.first }, { it.second }))
            val first = orderedTimes.first()
            val timesCsv = orderedTimes.joinToString(",") { (h, m) -> String.format("%02d:%02d", h, m) }

            if (editingMedicineId != null) {
                val existing = repository.getMedicine(editingMedicineId) ?: return@launch
                AlarmScheduler.cancel(getApplication(), existing)
                AlarmScheduler.cancelEndDate(getApplication(), existing.medicineId)
                val updated = existing.copy(
                    name = name,
                    dosage = dosage,
                    reminderHour = first.first,
                    reminderMinute = first.second,
                    frequency = frequency,
                    specificDaysCsv = specificDays.joinToString(","),
                    everyXHours = everyXHours,
                    startDateEpochDay = startDateEpochDay,
                    endDateEpochDay = endDateEpochDay,
                    colorHex = colorHex,
                    icon = icon,
                    missedAfterMinutes = missedAfterMinutes,
                    timesCsv = timesCsv,
                    quantityAvailable = quantityAvailable,
                    dosesPerIntake = dosesPerIntake
                )
                repository.updateMedicine(updated)
                if (frequency != Frequency.SOS) {
                    AlarmScheduler.scheduleNext(getApplication(), updated)
                    AlarmScheduler.scheduleEndDate(getApplication(), updated)
                }
            } else {
                val medicine = Medicine(
                    name = name,
                    dosage = dosage,
                    reminderHour = first.first,
                    reminderMinute = first.second,
                    frequency = frequency,
                    specificDaysCsv = specificDays.joinToString(","),
                    everyXHours = everyXHours,
                    startDateEpochDay = startDateEpochDay,
                    endDateEpochDay = endDateEpochDay,
                    colorHex = colorHex,
                    icon = icon,
                    missedAfterMinutes = missedAfterMinutes,
                    timesCsv = timesCsv,
                    quantityAvailable = quantityAvailable,
                    dosesPerIntake = dosesPerIntake
                )
                val id = repository.addMedicine(medicine)
                if (frequency != Frequency.SOS) {
                    val saved = medicine.copy(medicineId = id)
                    AlarmScheduler.scheduleNext(getApplication(), saved)
                    AlarmScheduler.scheduleEndDate(getApplication(), saved)
                }
            }
            onSaved()
        }
    }

    fun deleteMedicine(medicine: Medicine, onDeleted: () -> Unit) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), medicine)
            AlarmScheduler.cancelEndDate(getApplication(), medicine.medicineId)
            repository.deleteMedicine(medicine)
            onDeleted()
        }
    }
}
