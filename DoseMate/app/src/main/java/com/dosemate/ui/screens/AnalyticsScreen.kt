package com.dosemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosemate.analytics.MedicineStats
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.components.TrendLineChart
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.ui.theme.StatusMissed
import com.dosemate.ui.theme.StatusTaken
import com.dosemate.viewmodel.AnalyticsViewModel

@Composable
fun AnalyticsScreen() {
    val viewModel: AnalyticsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val colors = LocalDoseMateColors.current

    GlassBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.headerText)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Monthly Adherence", "${state.stats.adherencePercent}%", Modifier.weight(1f))
                    StatTile("Avg Delay", "${state.stats.averageDelayMinutes} min", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Current Streak", "${state.stats.currentStreakDays} days", Modifier.weight(1f))
                    StatTile("Missed (30d)", "${state.stats.missedCount}", Modifier.weight(1f))
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Medication Timing Trend", fontWeight = FontWeight.SemiBold)
                    Text("Minutes late per day", fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    TrendLineChart(points = state.timingTrend, lineColor = colors.accent)
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Adherence Trend", fontWeight = FontWeight.SemiBold)
                    Text("Weekly adherence %", fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    TrendLineChart(points = state.adherenceTrend, lineColor = StatusTaken)
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Smart Insights", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    state.insights.forEach { insight ->
                        Text("• $insight", fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }

            item {
                Text("Medicine-specific Analysis", fontWeight = FontWeight.SemiBold, color = colors.headerText, fontSize = 16.sp)
            }

            items(state.perMedicine) { stats -> MedicineStatRow(stats) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier = modifier) {
        Text(label, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LocalDoseMateColors.current.accent)
    }
}

@Composable
private fun MedicineStatRow(stats: MedicineStats) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stats.medicineName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            stats.usualTakenTimeLabel?.let { "Usually taken around $it" } ?: "No doses logged yet",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Avg delay: ${stats.averageDelayMinutes} min", fontSize = 12.sp)
            Text("Missed: ${stats.missedCount}", fontSize = 12.sp, color = StatusMissed)
            val consistencyText = if (stats.consistencyScore < 0) "No data" else "${stats.consistencyScore}%"
            Text("Consistency: $consistencyText", fontSize = 12.sp)
        }
    }
}
