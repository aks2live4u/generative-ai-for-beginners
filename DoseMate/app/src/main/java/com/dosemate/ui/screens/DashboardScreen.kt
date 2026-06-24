package com.dosemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosemate.data.LogStatus
import com.dosemate.data.Medicine
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.components.StatusPill
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.viewmodel.DashboardViewModel
import com.dosemate.viewmodel.DoseSlot
import com.dosemate.viewmodel.SosEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onEditMedicine: (Long) -> Unit
) {
    val viewModel: DashboardViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    GlassBackdrop {
        val headerColor = LocalDoseMateColors.current.headerText
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    if (state.isToday) "Today's Medicines" else "Medicines",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                )
            }

            item {
                WeekStrip(
                    selectedDate = state.selectedDate,
                    onPreviousWeek = viewModel::goToPreviousWeek,
                    onNextWeek = viewModel::goToNextWeek,
                    onSelectDate = viewModel::selectDate
                )
            }

            if (!state.isToday) {
                item {
                    Text(
                        "Viewing ${state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))} — log any doses you missed.",
                        fontSize = 12.sp,
                        color = LocalDoseMateColors.current.textSecondary
                    )
                }
            }

            if (state.lowStockMedicines.isNotEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("⚠️ Running Low", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        state.lowStockMedicines.forEach {
                            Text("${it.name} — restock soon (${it.quantityAvailable} left)", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state.slots.isEmpty() && state.sosEntries.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No medicines scheduled for this day. Add your first one from the Add tab.")
                    }
                }
            }

            items(state.slots) { slot ->
                DoseSlotCard(
                    slot,
                    onTaken = { viewModel.logSlotTaken(slot) },
                    onSkip = { viewModel.logSlotSkipped(slot) },
                    onEdit = { onEditMedicine(slot.medicine.medicineId) },
                    onDelete = { medicineToDelete = slot.medicine }
                )
            }

            items(state.sosEntries) { entry ->
                SosEntryCard(
                    entry,
                    onLog = { viewModel.logSosDose(entry.medicine) },
                    onEdit = { onEditMedicine(entry.medicine.medicineId) },
                    onDelete = { medicineToDelete = entry.medicine }
                )
            }
        }
    }

    medicineToDelete?.let { medicine ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text("Delete ${medicine.name}?") },
            text = { Text("This removes the medicine and its reminders. Past history stays in your logs.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMedicine(medicine)
                    medicineToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { medicineToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WeekStrip(
    selectedDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val colors = LocalDoseMateColors.current
    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    val today = LocalDate.now()

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onPreviousWeek) { Text("‹", fontSize = 20.sp, color = colors.headerText) }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..6) {
                    val date = weekStart.plusDays(i.toLong())
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectDate(date) }
                            .background(if (isSelected) colors.accent.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.accent else androidx.compose.ui.graphics.Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onNextWeek) { Text("›", fontSize = 20.sp, color = colors.headerText) }
        }
    }
}

@Composable
private fun DoseSlotCard(
    slot: DoseSlot,
    onTaken: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val medicine = slot.medicine
    val log = slot.log
    val timeText = String.format("%02d:%02d", slot.slotHour, slot.slotMinute)
    val takenTimeText = log?.actualTakenEpochMillis?.let {
        DateTimeFormatter.ofPattern("h:mm a").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    }
    val iconSurface = LocalDoseMateColors.current.iconSurface
    val isResolved = log != null && log.status != LogStatus.PENDING

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(48.dp).background(iconSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(medicine.icon, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(medicine.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                val scheduleText = "${medicine.dosage} · Scheduled $timeText"
                Text(scheduleText + (takenTimeText?.let { " · Taken $it" } ?: ""), fontSize = 12.sp)
            }
            StatusPill(status = log?.status ?: LogStatus.PENDING)
            CardMenu(onEdit = onEdit, onDelete = onDelete)
        }

        if (!isResolved) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onTaken, colors = ButtonDefaults.buttonColors(containerColor = LocalDoseMateColors.current.accent)) {
                    Text("✓ Log as Taken", fontSize = 13.sp)
                }
                OutlinedButton(onClick = onSkip) {
                    Text("Skip", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SosEntryCard(
    entry: SosEntry,
    onLog: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val medicine = entry.medicine
    val accent = LocalDoseMateColors.current.accent
    val now = System.currentTimeMillis()
    val cooldownActive = entry.cooldownUntilMillis != null && entry.cooldownUntilMillis > now
    val remainingMinutes = if (cooldownActive) ((entry.cooldownUntilMillis!! - now) / 60_000L).toInt() else 0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(48.dp).background(LocalDoseMateColors.current.iconSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(medicine.icon, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(medicine.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                val doseCountText = if (entry.logsToday.isNotEmpty()) " · Logged ${entry.logsToday.size}x today" else ""
                Text("${medicine.dosage} · As needed$doseCountText", fontSize = 12.sp)
            }
            Text("As needed", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Medium)
            CardMenu(onEdit = onEdit, onDelete = onDelete)
        }

        Spacer(Modifier.height(12.dp))
        if (cooldownActive) {
            Text(
                "Logged. Available again in ${remainingMinutes / 60}h ${remainingMinutes % 60}m.",
                fontSize = 12.sp,
                color = LocalDoseMateColors.current.textSecondary
            )
        } else {
            Button(onClick = onLog, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Text("✓ Log dose now", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CardMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Text("⋮", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
        }
    }
}
