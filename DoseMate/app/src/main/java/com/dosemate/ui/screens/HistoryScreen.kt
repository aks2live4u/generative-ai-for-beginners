package com.dosemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.dosemate.data.MedicineLog
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.components.StatusPill
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.viewmodel.HistoryFilter
import com.dosemate.viewmodel.HistoryViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = viewModel()
    val logs by viewModel.logs.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()

    GlassBackdrop {
        val headerColor = LocalDoseMateColors.current.headerText
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("History", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = headerColor)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HistoryFilter.TODAY to "Today", HistoryFilter.WEEK to "Week", HistoryFilter.MONTH to "Month")
                        .forEach { (value, label) ->
                            FilterChip(
                                selected = currentFilter == value,
                                onClick = { viewModel.setFilter(value) },
                                label = { Text(label) }
                            )
                        }
                }
            }

            if (logs.isEmpty()) {
                item { GlassCard(modifier = Modifier.fillMaxWidth()) { Text("No history in this range yet.") } }
            }

            val grouped = logs.groupBy { it.dateEpochDay }.toSortedMap(compareByDescending { it })
            grouped.forEach { (day, dayLogs) ->
                item {
                    Text(
                        DateTimeFormatter.ofPattern("MMMM d").format(LocalDate.ofEpochDay(day)),
                        fontWeight = FontWeight.SemiBold,
                        color = headerColor,
                        fontSize = 14.sp
                    )
                }
                items(dayLogs) { log -> HistoryRow(log) }
            }
        }
    }
}

@Composable
private fun HistoryRow(log: MedicineLog) {
    val scheduled = DateTimeFormatter.ofPattern("h:mm a")
        .format(Instant.ofEpochMilli(log.scheduledEpochMillis).atZone(ZoneId.systemDefault()))
    val taken = log.actualTakenEpochMillis?.let {
        DateTimeFormatter.ofPattern("h:mm a").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(log.medicineName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val detail = "Scheduled: $scheduled" + (taken?.let { " · Taken: $it" } ?: "") +
                    (log.delayMinutes?.let { " (+${it} min)" } ?: "")
                Text(detail, fontSize = 12.sp)
            }
            StatusPill(status = log.status)
        }
    }
}
