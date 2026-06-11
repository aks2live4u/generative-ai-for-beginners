package com.notnow.app.ui.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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

private enum class StatFilter { FRICTION, WENT_BACK, NIGHT_BLOCKED, EMERGENCY }

private fun UsageRecord.matches(filter: StatFilter) = when (filter) {
    StatFilter.FRICTION     -> outcome == AccessOutcome.WAITED || outcome == AccessOutcome.WENT_BACK || outcome == AccessOutcome.NIGHT_BLOCKED
    StatFilter.WENT_BACK    -> outcome == AccessOutcome.WENT_BACK
    StatFilter.NIGHT_BLOCKED -> outcome == AccessOutcome.NIGHT_BLOCKED
    StatFilter.EMERGENCY    -> outcome == AccessOutcome.EMERGENCY_UNLOCKED
}

private fun StatFilter.label() = when (this) {
    StatFilter.FRICTION      -> "Friction Events"
    StatFilter.WENT_BACK     -> "Went Back"
    StatFilter.NIGHT_BLOCKED -> "Night Blocked"
    StatFilter.EMERGENCY     -> "Emergency Unlocks"
}

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

    var selectedFilter by remember { mutableStateOf<StatFilter?>(null) }
    var showHourlyBreakdown by remember { mutableStateOf(false) }

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
                    StatCard(
                        "$totalFriction", "Friction\nEvents", Modifier.weight(1f), AccentAmber,
                        selected = selectedFilter == StatFilter.FRICTION,
                        onClick = { selectedFilter = if (selectedFilter == StatFilter.FRICTION) null else StatFilter.FRICTION }
                    )
                    StatCard(
                        "$wentBack", "Went\nBack", Modifier.weight(1f), AccentGreen,
                        selected = selectedFilter == StatFilter.WENT_BACK,
                        onClick = { selectedFilter = if (selectedFilter == StatFilter.WENT_BACK) null else StatFilter.WENT_BACK }
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        "$nightBlocked", "Night\nBlocked", Modifier.weight(1f), AccentBlue,
                        selected = selectedFilter == StatFilter.NIGHT_BLOCKED,
                        onClick = { selectedFilter = if (selectedFilter == StatFilter.NIGHT_BLOCKED) null else StatFilter.NIGHT_BLOCKED }
                    )
                    StatCard(
                        "$emergency", "Emergency\nUnlocks", Modifier.weight(1f), AccentRed,
                        selected = selectedFilter == StatFilter.EMERGENCY,
                        onClick = { selectedFilter = if (selectedFilter == StatFilter.EMERGENCY) null else StatFilter.EMERGENCY }
                    )
                }
            }

            if (state.peakHour != "—") {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardDark,
                        onClick = { showHourlyBreakdown = !showHourlyBreakdown }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Peak Trigger Time", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(state.peakHour, style = MaterialTheme.typography.titleMedium, color = AccentAmber)
                                        Text("${state.peakHourCount} events", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Icon(
                                        if (showHourlyBreakdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        null, tint = TextSecondary
                                    )
                                }
                            }
                            if (showHourlyBreakdown) {
                                Spacer(Modifier.height(12.dp))
                                val maxCount = state.hourlyBreakdown.maxOfOrNull { it.count } ?: 1
                                state.hourlyBreakdown.forEach { hc ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(hc.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.width(64.dp))
                                        Box(Modifier.weight(1f).height(8.dp).background(SurfaceDark, RoundedCornerShape(4.dp))) {
                                            Box(
                                                Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(hc.count.toFloat() / maxCount.toFloat())
                                                    .background(AccentAmber, RoundedCornerShape(4.dp))
                                            )
                                        }
                                        Text("${hc.count}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.padding(start = 8.dp).width(24.dp), textAlign = TextAlign.End)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent activity
            val displayRecords = if (selectedFilter != null) {
                state.recentRecords.filter { it.matches(selectedFilter!!) }
            } else {
                state.recentRecords.take(20)
            }

            if (state.recentRecords.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            selectedFilter?.let { "${it.label()} (${displayRecords.size})" } ?: "Recent Activity",
                            style = MaterialTheme.typography.titleLarge, color = TextPrimary
                        )
                        if (selectedFilter != null) {
                            TextButton(onClick = { selectedFilter = null }) {
                                Text("Clear filter", color = AccentAmber)
                            }
                        }
                    }
                }
                if (displayRecords.isEmpty()) {
                    item {
                        Text(
                            "No events for this filter in the last 7 days.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                }
                items(displayRecords, key = { it.id }) { record ->
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
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDark,
        border = if (selected) BorderStroke(2.dp, color) else null,
        onClick = onClick,
        modifier = modifier
    ) {
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
