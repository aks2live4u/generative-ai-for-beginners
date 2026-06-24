package com.dosemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.Frequency
import com.dosemate.data.LogStatus
import com.dosemate.data.Medicine
import com.dosemate.data.MedicineLog
import com.dosemate.data.timesOfDay
import com.dosemate.scheduling.AlarmScheduler
import com.dosemate.scheduling.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val SOS_COOLDOWN_MILLIS = Duration.ofHours(8).toMillis()
private const val LOW_STOCK_DAYS_THRESHOLD = 3

data class DoseSlot(
    val medicine: Medicine,
    val slotHour: Int,
    val slotMinute: Int,
    val scheduledEpochMillis: Long,
    val log: MedicineLog?
)

data class SosEntry(
    val medicine: Medicine,
    val logsToday: List<MedicineLog>,
    val cooldownUntilMillis: Long?
)

data class DashboardUiState(
    val selectedDate: LocalDate,
    val isToday: Boolean,
    val slots: List<DoseSlot>,
    val sosEntries: List<SosEntry>,
    val lowStockMedicines: List<Medicine>
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoseMateRepository(application)
    private val zone = ZoneId.systemDefault()

    private val selectedDate = MutableStateFlow(LocalDate.now())

    private val logsForSelectedDate = selectedDate.flatMapLatest { date ->
        repository.observeLogsForDay(date.toEpochDay())
    }

    val uiState = combine(
        repository.observeMedicines(),
        logsForSelectedDate,
        repository.observeAllLogs(),
        selectedDate
    ) { medicines, dayLogs, allLogs, date ->
        val active = medicines.filter { it.isActive }
        val today = LocalDate.now()

        val slots = active.filter { it.frequency != Frequency.SOS }
            .flatMap { medicine -> slotsForDate(medicine, date).map { time -> medicine to time } }
            .map { (medicine, time) ->
                val (h, m) = time
                val scheduledMillis = date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
                val log = dayLogs.filter { it.medicineId == medicine.medicineId }
                    .firstOrNull { sameClockTime(it.scheduledEpochMillis, h, m) }
                DoseSlot(medicine, h, m, scheduledMillis, log)
            }
            .sortedWith(compareBy({ it.slotHour }, { it.slotMinute }, { it.medicine.name }))

        val sosEntries = active.filter { it.frequency == Frequency.SOS }
            .map { medicine ->
                val logsToday = dayLogs.filter { it.medicineId == medicine.medicineId }
                    .sortedByDescending { it.scheduledEpochMillis }
                val lastTaken = allLogs.filter { it.medicineId == medicine.medicineId && it.status == LogStatus.TAKEN }
                    .maxByOrNull { it.actualTakenEpochMillis ?: 0L }
                val cooldownUntil = lastTaken?.actualTakenEpochMillis?.plus(SOS_COOLDOWN_MILLIS)
                SosEntry(medicine, logsToday, cooldownUntil)
            }

        val lowStock = active.filter { medicine ->
            val qty = medicine.quantityAvailable
            if (qty == null || medicine.frequency == Frequency.SOS) {
                false
            } else {
                val dosesPerDay = slotsForDate(medicine, today).size.coerceAtLeast(1)
                val daysLeft = qty / (dosesPerDay * medicine.dosesPerIntake).coerceAtLeast(1)
                daysLeft <= LOW_STOCK_DAYS_THRESHOLD
            }
        }

        DashboardUiState(date, date == today, slots, sosEntries, lowStock)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState(LocalDate.now(), true, emptyList(), emptyList(), emptyList())
    )

    fun goToPreviousWeek() { selectedDate.value = selectedDate.value.minusWeeks(1) }
    fun goToNextWeek() {
        val next = selectedDate.value.plusWeeks(1)
        selectedDate.value = if (next.isAfter(LocalDate.now())) LocalDate.now() else next
    }
    fun selectDate(date: LocalDate) {
        if (!date.isAfter(LocalDate.now())) selectedDate.value = date
    }
    fun goToToday() { selectedDate.value = LocalDate.now() }

    fun logSlotTaken(slot: DoseSlot) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val delay = ((now - slot.scheduledEpochMillis) / 60_000L).toInt().coerceAtLeast(0)
            if (slot.log != null) {
                repository.updateLog(slot.log.copy(status = LogStatus.TAKEN, actualTakenEpochMillis = now, delayMinutes = delay))
            } else {
                repository.insertLog(
                    MedicineLog(
                        medicineId = slot.medicine.medicineId,
                        medicineName = slot.medicine.name,
                        scheduledEpochMillis = slot.scheduledEpochMillis,
                        actualTakenEpochMillis = now,
                        status = LogStatus.TAKEN,
                        delayMinutes = delay,
                        dateEpochDay = selectedDate.value.toEpochDay()
                    )
                )
            }
            applyStockUsage(slot.medicine)
        }
    }

    fun logSlotSkipped(slot: DoseSlot) {
        viewModelScope.launch {
            if (slot.log != null) {
                repository.updateLog(slot.log.copy(status = LogStatus.SKIPPED))
            } else {
                repository.insertLog(
                    MedicineLog(
                        medicineId = slot.medicine.medicineId,
                        medicineName = slot.medicine.name,
                        scheduledEpochMillis = slot.scheduledEpochMillis,
                        status = LogStatus.SKIPPED,
                        dateEpochDay = selectedDate.value.toEpochDay()
                    )
                )
            }
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
                    dateEpochDay = selectedDate.value.toEpochDay()
                )
            )
            applyStockUsage(medicine)
        }
    }

    /** Marks an as-needed (SOS) medicine as deliberately not taken today, independent of the cooldown. */
    fun logSosSkipped(medicine: Medicine) {
        viewModelScope.launch {
            repository.insertLog(
                MedicineLog(
                    medicineId = medicine.medicineId,
                    medicineName = medicine.name,
                    scheduledEpochMillis = System.currentTimeMillis(),
                    status = LogStatus.SKIPPED,
                    dateEpochDay = selectedDate.value.toEpochDay()
                )
            )
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), medicine)
            repository.deleteMedicine(medicine)
        }
    }

    private suspend fun applyStockUsage(medicine: Medicine) {
        if (medicine.quantityAvailable == null) return
        val updated = repository.decrementStock(medicine.medicineId, medicine.dosesPerIntake) ?: return
        val dosesPerDay = if (medicine.frequency == Frequency.SOS) 1 else slotsForDate(medicine, LocalDate.now()).size.coerceAtLeast(1)
        val daysLeft = updated.quantityAvailable?.div((dosesPerDay * medicine.dosesPerIntake).coerceAtLeast(1))
        if (daysLeft != null && daysLeft <= LOW_STOCK_DAYS_THRESHOLD) {
            NotificationHelper.showLowStock(getApplication(), medicine.medicineId, medicine.name, daysLeft)
        }
    }

    private fun sameClockTime(epochMillis: Long, hour: Int, minute: Int): Boolean {
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return time.hour == hour && time.minute == minute
    }

    private fun slotsForDate(medicine: Medicine, date: LocalDate): List<Pair<Int, Int>> {
        if (medicine.frequency == Frequency.SOS) return emptyList()
        val startDate = LocalDate.ofEpochDay(medicine.startDateEpochDay)
        if (date.isBefore(startDate)) return emptyList()
        medicine.endDateEpochDay?.let { if (date.isAfter(LocalDate.ofEpochDay(it))) return emptyList() }
        return when (medicine.frequency) {
            Frequency.DAILY -> medicine.timesOfDay()
            Frequency.SPECIFIC_DAYS -> {
                val days = medicine.specificDaysCsv.split(",").filter { it.isNotBlank() }
                    .map { DayOfWeek.of(it.trim().toInt()) }.toSet()
                if (days.contains(date.dayOfWeek)) medicine.timesOfDay() else emptyList()
            }
            Frequency.ALTERNATE_DAYS -> {
                val onCycle = java.time.temporal.ChronoUnit.DAYS.between(startDate, date) % 2 == 0L
                if (onCycle) medicine.timesOfDay() else emptyList()
            }
            Frequency.EVERY_X_HOURS -> everyXHourSlotsForDate(medicine, startDate, date)
            Frequency.SOS -> emptyList()
        }
    }

    private fun everyXHourSlotsForDate(medicine: Medicine, startDate: LocalDate, date: LocalDate): List<Pair<Int, Int>> {
        val interval = medicine.everyXHours.coerceAtLeast(1).toLong()
        val (startH, startM) = medicine.timesOfDay().first()
        val startDateTime = startDate.atTime(startH, startM)
        val dateStart = date.atStartOfDay()
        var cursor = if (dateStart.isBefore(startDateTime)) {
            startDateTime
        } else {
            val hoursBetween = Duration.between(startDateTime, dateStart).toHours()
            val steps = hoursBetween / interval
            startDateTime.plusHours(steps * interval)
        }
        while (cursor.toLocalDate().isBefore(date)) cursor = cursor.plusHours(interval)
        val result = mutableListOf<Pair<Int, Int>>()
        var guard = 0
        while (cursor.toLocalDate() == date && guard < 48) {
            result.add(cursor.hour to cursor.minute)
            cursor = cursor.plusHours(interval)
            guard++
        }
        return result
    }
}
