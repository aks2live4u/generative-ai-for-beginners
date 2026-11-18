package com.dosemate.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.Frequency
import com.dosemate.data.Medicine
import com.dosemate.data.ParsedMedicine
import com.dosemate.data.PrescriptionParser
import com.dosemate.scheduling.AlarmScheduler
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

private val SCAN_COLORS = listOf(
    "#0F9B8E", // teal (default)
    "#7C3AED", // violet
    "#DC2626", // red
    "#D97706", // amber
    "#2563EB", // blue
    "#059669", // emerald
    "#DB2777", // pink
    "#EA580C"  // orange
)

sealed class ScanState {
    object Camera : ScanState()
    object Processing : ScanState()
    data class Review(
        val parsed: List<ParsedMedicine>,
        val existingActive: List<Medicine>
    ) : ScanState()
    data class Error(val message: String) : ScanState()
    object Applied : ScanState()
}

class ScanPrescriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoseMateRepository(application)

    private val _state = MutableStateFlow<ScanState>(ScanState.Camera)
    val state: StateFlow<ScanState> = _state

    fun processImage(bitmap: Bitmap) {
        _state.value = ScanState.Processing
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                recognizer.close()
                val parsed = PrescriptionParser.parse(visionText.text)
                viewModelScope.launch {
                    val existing = repository.getActiveMedicines()
                    _state.value = if (parsed.isEmpty()) {
                        ScanState.Error("No medicines detected. Try a clearer, well-lit photo of the prescription.")
                    } else {
                        ScanState.Review(parsed, existing)
                    }
                }
            }
            .addOnFailureListener { e ->
                recognizer.close()
                _state.value = ScanState.Error("Could not process the image: ${e.localizedMessage ?: "unknown error"}")
            }
    }

    fun applyPrescription(proposed: List<ParsedMedicine>, archiveUnmatched: Boolean) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val existingActive = repository.getActiveMedicines()
            val retainedIds = mutableSetOf<Long>()
            var colorIndex = 0

            proposed.forEach { parsed ->
                val nameL = parsed.name.lowercase()
                val match = existingActive.firstOrNull { existing ->
                    val existingL = existing.name.lowercase()
                    existingL.contains(nameL) || nameL.contains(existingL)
                }

                val firstTime = parsed.timesOfDay.firstOrNull() ?: (9 to 0)
                val timesCsv = parsed.timesOfDay.joinToString(",") { (h, m) ->
                    String.format("%02d:%02d", h, m)
                }

                if (match != null) {
                    retainedIds.add(match.medicineId)
                    AlarmScheduler.cancel(getApplication(), match)
                    val updated = match.copy(
                        dosage = parsed.dosage,
                        frequency = parsed.frequency,
                        reminderHour = firstTime.first,
                        reminderMinute = firstTime.second,
                        timesCsv = timesCsv,
                        everyXHours = parsed.everyXHours,
                        isArchived = false
                    )
                    repository.updateMedicine(updated)
                    if (parsed.frequency != Frequency.SOS) {
                        AlarmScheduler.scheduleNext(getApplication(), updated)
                    }
                } else {
                    val medicine = Medicine(
                        name = parsed.name,
                        dosage = parsed.dosage,
                        reminderHour = firstTime.first,
                        reminderMinute = firstTime.second,
                        frequency = parsed.frequency,
                        everyXHours = parsed.everyXHours,
                        startDateEpochDay = today,
                        timesCsv = timesCsv,
                        colorHex = SCAN_COLORS[colorIndex % SCAN_COLORS.size]
                    )
                    colorIndex++
                    val id = repository.addMedicine(medicine)
                    retainedIds.add(id)
                    if (parsed.frequency != Frequency.SOS) {
                        AlarmScheduler.scheduleNext(getApplication(), medicine.copy(medicineId = id))
                    }
                }
            }

            if (archiveUnmatched) {
                repository.archiveMedicinesExcept(retainedIds)
            }

            _state.value = ScanState.Applied
        }
    }

    fun retryCamera() { _state.value = ScanState.Camera }
}
