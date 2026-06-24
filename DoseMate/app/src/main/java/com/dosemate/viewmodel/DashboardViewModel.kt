package com.dosemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.LogStatus
import com.dosemate.data.Medicine
import com.dosemate.data.MedicineLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayEntry(
    val medicine: Medicine,
    val log: MedicineLog?
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoseMateRepository(application)
    private val today = LocalDate.now().toEpochDay()

    val todayEntries = combine(
        repository.observeMedicines(),
        repository.observeLogsForDay(today)
    ) { medicines, logs ->
        medicines.filter { it.isActive }.map { medicine ->
            val log = logs.filter { it.medicineId == medicine.medicineId }
                .maxByOrNull { it.scheduledEpochMillis }
            TodayEntry(medicine, log)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markTaken(log: MedicineLog) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val delay = ((now - log.scheduledEpochMillis) / 60_000L).toInt().coerceAtLeast(0)
            repository.updateLog(log.copy(status = LogStatus.TAKEN, actualTakenEpochMillis = now, delayMinutes = delay))
        }
    }

    fun markSkipped(log: MedicineLog) {
        viewModelScope.launch {
            repository.updateLog(log.copy(status = LogStatus.SKIPPED))
        }
    }

    /** Logs an as-needed (SOS) dose taken right now, for trend analysis. */
    fun logSosDose(medicine: Medicine) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.insertLog(
                MedicineLog(
                    medicineId = medicine.medicineId,
                    medicineName = medicine.name,
                    scheduledEpochMillis = now,
                    actualTakenEpochMillis = now,
                    status = LogStatus.TAKEN,
                    delayMinutes = 0,
                    dateEpochDay = LocalDate.now().toEpochDay()
                )
            )
        }
    }
}
