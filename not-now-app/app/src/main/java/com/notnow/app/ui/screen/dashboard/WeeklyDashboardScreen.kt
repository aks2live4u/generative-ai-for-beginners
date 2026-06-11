package com.notnow.app.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notnow.app.NotNowApplication
import com.notnow.app.data.entity.AccessOutcome
import com.notnow.app.data.entity.UsageRecord
import com.notnow.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NotNowApplication
    val vm: WeeklyDashboardViewModel = viewModel(
        factory = WeeklyDashboardViewModel.Factory(app.usageRepository, app.preferences)
    )

    val state by vm.uiState.collectAsStateWithLifecycle()

    val waited = state.recentRecords.count { it.outcome == AccessOutcome.WAITED }
    val wentBack = state.recentRecords.count { it.outcome == AccessOutcome.WENT_BACK }
    val emergency = state.recentRecords.count { it.outcome == AccessOutcome.EMERGENCY_UNLOCKED }
    val nightBlocked = state.recentRecords.count { it.outcome == AccessOutcome.NIGHT_BLOCKED }
    val totalFriction = waited + wentBack + nightBlocked

    val fmt = remember { SimpleDateFormat("EEE h:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            TopAppBar(
                title = { Text("Weekly Review", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats grid
            item {
                Text("Last 7 Days", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("$totalFriction", "Friction\nEvents", Modifier.weight(1f), AccentAmber)
                    StatCard("$wentBack", "Went\nBack", Modifier.weight(1f), AccentGreen)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("$nightBlocked", "Night\nBlocked", Modifier.weight(1f), AccentBlue)
                    StatCard("$emergency", "Emergency\nUnlocks", Modifier.weight(1f), AccentRed)
                }
            }

            if (state.peakHour != "—") {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = CardDark) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Peak Trigger Time", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(state.peakHour, style = MaterialTheme.typography.titleMedium, color = AccentAmber)
                                Text("${state.peakHourCount} events", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Recent activity
            if (state.recentRecords.isNotEmpty()) {
                item {
                    Text("Recent Activity", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                }
                items(state.recentRecords.take(20), key = { it.id }) { record ->
                    ActivityRow(record = record, fmt = fmt)
                }
            } else {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", style = MaterialTheme.typography.headlineLarge)
                        Text("No data yet", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                        Text("Activity appears here after friction events occur.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardDark, modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineLarge, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ActivityRow(record: UsageRecord, fmt: SimpleDateFormat) {
    val (emoji, color) = when (record.outcome) {
        AccessOutcome.WAITED             -> "⏱️" to AccentAmber
        AccessOutcome.WENT_BACK          -> "✅" to AccentGreen
        AccessOutcome.EMERGENCY_UNLOCKED -> "🚨" to AccentRed
        AccessOutcome.NIGHT_BLOCKED      -> "🌙" to AccentBlue
    }
    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceDark) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(record.appName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(record.outcome.label(), style = MaterialTheme.typography.bodyMedium, color = color)
            }
            Text(fmt.format(Date(record.attemptedAt)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

private fun AccessOutcome.label() = when(this) {
    AccessOutcome.WAITED             -> "Waited it out"
    AccessOutcome.WENT_BACK          -> "Chose to go back"
    AccessOutcome.EMERGENCY_UNLOCKED -> "Emergency unlock"
    AccessOutcome.NIGHT_BLOCKED      -> "Night locked"
}
