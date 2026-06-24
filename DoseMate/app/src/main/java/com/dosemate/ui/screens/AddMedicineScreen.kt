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
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.ui.theme.TealDeep
import com.dosemate.viewmodel.AddMedicineViewModel

private val dosagePresets = listOf("1 Tablet", "2 Tablets", "5 ml", "Custom")
private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun AddMedicineScreen(onSaved: () -> Unit) {
    val viewModel: AddMedicineViewModel = viewModel()

    var name by remember { mutableStateOf("") }
    var dosagePreset by remember { mutableStateOf(dosagePresets.first()) }
    var customDosage by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }
    var frequency by remember { mutableStateOf(Frequency.DAILY) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var everyXHours by remember { mutableIntStateOf(8) }
    var missedAfterMinutes by remember { mutableIntStateOf(60) }

    GlassBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Add Medicine", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalDoseMateColors.current.headerText)
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

            if (frequency != Frequency.SOS) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Reminder Time", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberStepper(value = hour, range = 0..23, onChange = { hour = it }, label = "Hour")
                            NumberStepper(value = minute, range = 0..59, onChange = { minute = it }, label = "Minute")
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

            item {
                Button(
                    onClick = {
                        val dosage = if (dosagePreset == "Custom") customDosage else dosagePreset
                        if (name.isNotBlank() && dosage.isNotBlank()) {
                            viewModel.saveMedicine(
                                name = name,
                                dosage = dosage,
                                reminderHour = hour,
                                reminderMinute = minute,
                                frequency = frequency,
                                specificDays = selectedDays,
                                everyXHours = everyXHours,
                                colorHex = "#0F9B8E",
                                icon = "💊",
                                missedAfterMinutes = missedAfterMinutes,
                                onSaved = onSaved
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                ) {
                    Text("Save", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun FrequencyOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = TealDeep))
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
