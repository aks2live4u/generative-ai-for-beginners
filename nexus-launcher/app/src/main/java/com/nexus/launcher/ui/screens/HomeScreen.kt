package com.nexus.launcher.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.system.NexusNotificationService
import com.nexus.launcher.system.Tier
import com.nexus.launcher.system.rememberBattery
import com.nexus.launcher.system.rememberNow
import com.nexus.launcher.ui.Screen
import com.nexus.launcher.ui.components.AppGlyph
import com.nexus.launcher.ui.components.GlassCard
import com.nexus.launcher.ui.components.HudLabel
import com.nexus.launcher.ui.components.RingGauge
import com.nexus.launcher.ui.components.StatusDot
import com.nexus.launcher.ui.theme.Nx
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DayPhase(val greeting: String, val prompts: List<String>) {
    MORNING("Good morning", listOf("Brief me on today's news", "What's the weather today?", "Start a 50 minute focus session")),
    AFTERNOON("Good afternoon", listOf("Start a 25 minute focus session", "Summarise my notifications", "What's the weather this afternoon?")),
    EVENING("Good evening", listOf("Summarise my notifications", "Set an alarm for 7am", "Recommend a film for tonight")),
    NIGHT("Late session", listOf("Set an alarm for 7am", "Wind down: focus for 20 minutes", "Tell me something fascinating")),
}

private fun phaseFor(hour: Int) = when (hour) {
    in 5..11 -> DayPhase.MORNING
    in 12..16 -> DayPhase.AFTERNOON
    in 17..21 -> DayPhase.EVENING
    else -> DayPhase.NIGHT
}

@Composable
fun HomeScreen(
    graph: NexusGraph,
    settings: NexusSettings,
    onNavigate: (Screen) -> Unit,
    onAsk: (String) -> Unit,
) {
    val context = LocalContext.current
    val now = rememberNow()
    val battery = rememberBattery()
    val focus by graph.focus.state.collectAsStateWithLifecycle()
    val apps by graph.apps.apps.collectAsStateWithLifecycle()
    val usage by graph.apps.usage.collectAsStateWithLifecycle()
    val notifications by NexusNotificationService.items.collectAsStateWithLifecycle()
    val phase = phaseFor(now.hour)
    val predicted = remember(apps, usage) { graph.apps.ranked(apps, usage).take(4) }
    val priorityCount = notifications.count { it.tier == Tier.PRIORITY }
    val listenerOn = remember(notifications) { NexusNotificationService.isEnabled(context) }

    // Adaptive suggestion: low battery takes the top slot.
    val lowBattery = battery.percent <= 20 && !battery.charging
    val suggestions = phase.prompts

    // Swiping past the top or bottom edge jumps to Apps (up) or Signals (down).
    val swipeNav = remember {
        object : NestedScrollConnection {
            var overscroll = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) overscroll += available.y
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                when {
                    overscroll < -140f -> onNavigate(Screen.ORBIT)
                    overscroll > 140f -> onNavigate(Screen.ALERTS)
                }
                overscroll = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .nestedScroll(swipeNav)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (settings.apiKey.isBlank()) Nx.Amber else Nx.Mint, settings.reducedMotion)
            Spacer(Modifier.width(8.dp))
            HudLabel(if (settings.apiKey.isBlank()) "NEXUS · AI offline" else "NEXUS · Online", Modifier.weight(1f))
            IconButton(onClick = { onNavigate(Screen.SETTINGS) }) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Nx.Muted)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.displayLarge)
        HudLabel(now.format(DateTimeFormatter.ofPattern("EEEE · d MMMM", Locale.getDefault())), color = Nx.Cyan)
        Spacer(Modifier.height(10.dp))
        Text(
            phase.greeting + if (settings.userName.isNotBlank()) ", ${settings.userName}." else ".",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(22.dp))
        GlassCard(Modifier.fillMaxWidth(), onClick = { onAsk("") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Nx.Cyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                HudLabel("NEXUS suggests", Modifier.weight(1f), color = Nx.Cyan)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Nx.Muted)
            }
            Spacer(Modifier.height(10.dp))
            if (lowBattery) {
                SuggestionLine("Battery at ${battery.percent}%: turn on Battery Saver") {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            suggestions.forEach { s -> SuggestionLine(s) { onAsk(s) } }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard(Modifier.weight(1f)) {
                HudLabel("Power")
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    RingGauge(battery.percent / 100f, if (lowBattery) Nx.Amber else Nx.Cyan, Modifier.fillMaxSize())
                    if (battery.charging) {
                        Icon(Icons.Outlined.BatteryChargingFull, contentDescription = "Charging", tint = Nx.Cyan, modifier = Modifier.size(22.dp))
                    } else {
                        Text("${battery.percent}", fontSize = 22.sp, fontWeight = FontWeight.Light)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(if (battery.charging) "Charging · ${battery.percent}%" else "${battery.percent}% remaining", style = MaterialTheme.typography.bodyMedium, color = Nx.Muted)
            }
            GlassCard(Modifier.weight(1f), onClick = { onNavigate(Screen.FOCUS) }) {
                HudLabel("Focus")
                Spacer(Modifier.height(12.dp))
                if (focus.active) {
                    val left = ((focus.endsAt - System.currentTimeMillis()).coerceAtLeast(0) / 60_000L) + 1
                    Text("$left", fontSize = 40.sp, fontWeight = FontWeight.Thin, color = Nx.Cyan)
                    Text("minutes left", style = MaterialTheme.typography.bodyMedium, color = Nx.Muted)
                } else {
                    Text("${focus.todayMinutes}", fontSize = 40.sp, fontWeight = FontWeight.Thin)
                    Text("minutes focused today", style = MaterialTheme.typography.bodyMedium, color = Nx.Muted)
                }
                Spacer(Modifier.height(10.dp))
                Text(if (focus.active) focus.label else "Tap to begin", style = MaterialTheme.typography.bodyMedium, color = Nx.Cyan)
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth(), onClick = { onNavigate(Screen.ALERTS) }, accent = if (priorityCount > 0) Nx.Amber else Nx.Cyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    HudLabel("Signals")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            !listenerOn -> "Let NEXUS sort your notifications"
                            priorityCount > 0 -> "$priorityCount need${if (priorityCount == 1) "s" else ""} you · ${notifications.size - priorityCount} in digest"
                            notifications.isEmpty() -> "All clear"
                            else -> "Nothing urgent · ${notifications.size} in digest"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Nx.Muted)
            }
        }

        if (predicted.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            HudLabel("Predicted for you")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                predicted.forEach { app ->
                    Column(
                        Modifier
                            .width(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) { graph.apps.launch(app) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppGlyph(app.icon, app.label, dimmed = !graph.focus.allows(app.packageName))
                        Spacer(Modifier.height(6.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        HudLabel("Swipe up for apps · down for signals", Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SuggestionLine(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("›", color = Nx.Cyan, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
