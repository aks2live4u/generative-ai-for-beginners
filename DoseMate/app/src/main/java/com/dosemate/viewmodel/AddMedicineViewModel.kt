package com.dosemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.Frequency
import com.dosemate.data.Medicine
import com.dosemate.scheduling.AlarmScheduler
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddMedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoseMateRepository(application)

    fun saveMedicine(
        name: String,
        dosage: String,
        reminderHour: Int,
        reminderMinute: Int,
        frequency: Frequency,
        specificDays: Set<Int>,
        everyXHours: Int,
        colorHex: String,
        icon: String,
        missedAfterMinutes: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val medicine = Medicine(
                name = name,
                dosage = dosage,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                frequency = frequency,
                specificDaysCsv = specificDays.joinToString(","),
                everyXHours = everyXHours,
                startDateEpochDay = LocalDate.now().toEpochDay(),
                colorHex = colorHex,
                icon = icon,
                missedAfterMinutes = missedAfterMinutes
            )
            val id = repository.addMedicine(medicine)
            if (frequency != Frequency.SOS) {
                AlarmScheduler.scheduleNext(getApplication(), medicine.copy(medicineId = id))
            }
            onSaved()
        }
    }
}
