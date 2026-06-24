package com.dosemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosemate.data.Frequency
import com.dosemate.data.Medicine
import com.dosemate.data.timesOfDay
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.viewmodel.AddMedicineViewModel
import java.time.LocalDate

private val dosagePresets = listOf("1 Tablet", "2 Tablets", "5 ml", "Custom")
private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun AddMedicineScreen(medicineId: Long? = null, onSaved: () -> Unit) {
    val viewModel: AddMedicineViewModel = viewModel()
    val accent = LocalDoseMateColors.current.accent

    var name by remember { mutableStateOf("") }
    var dosagePreset by remember { mutableStateOf(dosagePresets.first()) }
    var customDosage by remember { mutableStateOf("") }
    var times by remember { mutableStateOf(listOf(9 to 0)) }
    var newTimeHour by remember { mutableIntStateOf(9) }
    var newTimeMinute by remember { mutableIntStateOf(0) }
    var frequency by remember { mutableStateOf(Frequency.DAILY) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var everyXHours by remember { mutableIntStateOf(8) }
    var missedAfterMinutes by remember { mutableIntStateOf(60) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var quantityText by remember { mutableStateOf("") }
    var dosesPerIntake by remember { mutableIntStateOf(1) }
    var loaded by remember { mutableStateOf(medicineId == null) }

    LaunchedEffect(medicineId) {
        if (medicineId != null) {
            viewModel.loadMedicine(medicineId)?.let { m: Medicine ->
                name = m.name
                dosagePreset = if (dosagePresets.contains(m.dosage)) m.dosage else "Custom"
                customDosage = m.dosage
                times = m.timesOfDay()
                frequency = m.frequency
                selectedDays = m.specificDaysCsv.split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet()
                everyXHours = if (m.everyXHours > 0) m.everyXHours else 8
                missedAfterMinutes = m.missedAfterMinutes
                startDate = LocalDate.ofEpochDay(m.startDateEpochDay)
                quantityText = m.quantityAvailable?.toString() ?: ""
                dosesPerIntake = m.dosesPerIntake
            }
            loaded = true
        }
    }

    if (!loaded) return

    GlassBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    if (medicineId != null) "Edit Medicine" else "Add Medicine",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalDoseMateColors.current.headerText
                )
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Medicine Name", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Paroxetine") }
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Dosage", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dosagePresets.forEach { preset ->
                            FilterChip(
                                selected = dosagePreset == preset,
                                onClick = { dosagePreset = preset },
                                label = { Text(preset, fontSize = 12.sp) }
                            )
                        }
                    }
                    if (dosagePreset == "Custom") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customDosage,
                            onValueChange = { customDosage = it },
                            placeholder = { Text("e.g. 10 mg") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Frequency", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Column {
                        FrequencyOption("Daily", frequency == Frequency.DAILY) { frequency = Frequency.DAILY }
                        FrequencyOption("Specific Days", frequency == Frequency.SPECIFIC_DAYS) { frequency = Frequency.SPECIFIC_DAYS }
                        FrequencyOption("Alternate Days", frequency == Frequency.ALTERNATE_DAYS) { frequency = Frequency.ALTERNATE_DAYS }
                        FrequencyOption("Every X Hours", frequency == Frequency.EVERY_X_HOURS) { frequency = Frequency.EVERY_X_HOURS }
                        FrequencyOption("SOS (As Needed)", frequency == Frequency.SOS) { frequency = Frequency.SOS }
                    }

                    if (frequency == Frequency.SPECIFIC_DAYS) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dayLabels.forEachIndexed { index, label ->
                                val dayOrdinal = index + 1
                                FilterChip(
                                    selected = selectedDays.contains(dayOrdinal),
                                    onClick = {
                                        selectedDays = if (selectedDays.contains(dayOrdinal)) selectedDays - dayOrdinal else selectedDays + dayOrdinal
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    if (frequency == Frequency.EVERY_X_HOURS) {
                        Spacer(Modifier.height(8.dp))
                        NumberStepper(value = everyXHours, range = 1..24, onChange = { everyXHours = it }, label = "Every X hours")
                    }

                    if (frequency == Frequency.SOS) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No fixed reminder. You'll log each dose from the Dashboard when you take it, and it still counts toward your trends.",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (frequency != Frequency.SOS && frequency != Frequency.EVERY_X_HOURS) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Dose Times", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Add every time of day you take this medicine.", fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        times.forEachIndexed { index, (h, m) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(String.format("%02d:%02d", h, m), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                if (times.size > 1) {
                                    IconButton(onClick = { times = times.filterIndexed { i, _ -> i != index } }) {
                                        Text("✕", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberStepper(value = newTimeHour, range = 0..23, onChange = { newTimeHour = it }, label = "Hour")
                            NumberStepper(value = newTimeMinute, range = 0..59, onChange = { newTimeMinute = it }, label = "Minute")
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(onClick = {
                                if (times.none { it.first == newTimeHour && it.second == newTimeMinute }) {
                                    times = (times + (newTimeHour to newTimeMinute)).sortedWith(compareBy({ it.first }, { it.second }))
                                }
                            }) {
                                Text("+ Add time", fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Mark as missed after", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        NumberStepper(value = missedAfterMinutes, range = 15..180, step = 15, onChange = { missedAfterMinutes = it }, label = "Minutes")
                    }
                }
            }

            if (frequency == Frequency.EVERY_X_HOURS) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("First Dose Time", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberStepper(value = times.first().first, range = 0..23, onChange = { times = listOf(it to times.first().second) }, label = "Hour")
                            NumberStepper(value = times.first().second, range = 0..59, onChange = { times = listOf(times.first().first to it) }, label = "Minute")
                        }
                    }
                }
            }

            if (frequency != Frequency.SOS) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Started On", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberStepper(value = startDate.year, range = 2020..2035, onChange = { startDate = startDate.withYear(it) }, label = "Year")
                            NumberStepper(value = startDate.monthValue, range = 1..12, onChange = {
                                startDate = LocalDate.of(startDate.year, 1, 1).plusMonths((it - 1).toLong()).withDayOfMonth(minOf(startDate.dayOfMonth, LocalDate.of(startDate.year, it, 1).lengthOfMonth()))
                            }, label = "Month")
                            NumberStepper(value = startDate.dayOfMonth, range = 1..startDate.lengthOfMonth(), onChange = { startDate = startDate.withDayOfMonth(it) }, label = "Day")
                        }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Stock Tracking (optional)", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Get a low-stock alert about 3 days before you run out.", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input -> if (input.all { it.isDigit() }) quantityText = input },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tablets/doses available, e.g. 30") },
                        label = { Text("Quantity available") }
                    )
                    if (frequency != Frequency.SOS) {
                        Spacer(Modifier.height(10.dp))
                        NumberStepper(value = dosesPerIntake, range = 1..10, onChange = { dosesPerIntake = it }, label = "Units per dose")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val dosage = if (dosagePreset == "Custom") customDosage else dosagePreset
                        if (name.isNotBlank() && dosage.isNotBlank()) {
                            viewModel.saveMedicine(
                                editingMedicineId = medicineId,
                                name = name,
                                dosage = dosage,
                                times = times,
                                frequency = frequency,
                                specificDays = selectedDays,
                                everyXHours = everyXHours,
                                startDateEpochDay = startDate.toEpochDay(),
                                colorHex = "#0F9B8E",
                                icon = "💊",
                                missedAfterMinutes = missedAfterMinutes,
                                quantityAvailable = quantityText.toIntOrNull(),
                                dosesPerIntake = dosesPerIntake,
                                onSaved = onSaved
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Save", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun FrequencyOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalDoseMateColors.current.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = accent))
        Text(label, fontSize = 14.sp)
    }
}

@Composable
private fun NumberStepper(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit, label: String) {
    Column {
        Text(label, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - step).coerceIn(range)) }) { Text("–", fontSize = 18.sp) }
            Text(value.toString(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onChange((value + step).coerceIn(range)) }) { Text("+", fontSize = 18.sp) }
        }
    }
}
