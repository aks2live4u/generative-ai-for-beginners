package com.dosemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosemate.data.Frequency
import com.dosemate.data.LogStatus
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.components.StatusPill
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.ui.theme.TealDeep
import com.dosemate.viewmodel.DashboardViewModel
import com.dosemate.viewmodel.TodayEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onAddMedicine: () -> Unit,
    onViewHistory: () -> Unit,
    onViewAnalytics: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel()
    val entries by viewModel.todayEntries.collectAsState()

    GlassBackdrop {
        val headerColor = LocalDoseMateColors.current.headerText
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Today's Medicines",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                )
            }

            if (entries.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No medicines yet. Add your first one below.")
                    }
                }
            }

            items(entries) { entry ->
                MedicineTodayCard(
                    entry,
                    onTaken = { entry.log?.let(viewModel::markTaken) },
                    onSkip = { entry.log?.let(viewModel::markSkipped) },
                    onLogSos = { viewModel.logSosDose(entry.medicine) }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction("➕ Add\nMedicine", Modifier.weight(1f), onAddMedicine)
                    QuickAction("🕘 View\nHistory", Modifier.weight(1f), onViewHistory)
                    QuickAction("📊 Analytics", Modifier.weight(1f), onViewAnalytics)
                }
            }
        }
    }
}

@Composable
private fun MedicineTodayCard(entry: TodayEntry, onTaken: () -> Unit, onSkip: () -> Unit, onLogSos: () -> Unit) {
    val medicine = entry.medicine
    val log = entry.log
    val isSos = medicine.frequency == Frequency.SOS
    val timeText = String.format("%02d:%02d", medicine.reminderHour, medicine.reminderMinute)
    val takenTimeText = log?.actualTakenEpochMillis?.let {
        DateTimeFormatter.ofPattern("h:mm a").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    }
    val iconSurface = LocalDoseMateColors.current.iconSurface

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
                val scheduleText = if (isSos) "As needed" else "${medicine.dosage} · Scheduled $timeText"
                Text(scheduleText + (takenTimeText?.let { " · Taken $it" } ?: ""), fontSize = 12.sp)
            }
            if (isSos && log == null) {
                Text("As needed", fontSize = 12.sp, color = TealDeep, fontWeight = FontWeight.Medium)
            } else {
                StatusPill(status = log?.status ?: LogStatus.PENDING)
            }
        }

        if (isSos) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onLogSos, colors = ButtonDefaults.buttonColors(containerColor = TealDeep)) {
                Text("✓ Log dose now", fontSize = 13.sp)
            }
        } else if (log != null && log.status == LogStatus.PENDING) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onTaken, colors = ButtonDefaults.buttonColors(containerColor = TealDeep)) {
                    Text("✓ Taken", fontSize = 13.sp)
                }
                OutlinedButton(onClick = onSkip) {
                    Text("Skip", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier.height(80.dp).clickable(onClick = onClick), contentPadding = PaddingValues(8.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Medium)
        }
    }
}

