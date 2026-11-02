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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.system.Sfx
import com.nexus.launcher.system.rememberNow
import com.nexus.launcher.ui.components.AppGlyph
import com.nexus.launcher.ui.components.GlassCard
import com.nexus.launcher.ui.components.HudLabel
import com.nexus.launcher.ui.components.NexusOrb
import com.nexus.launcher.ui.components.OrbMode
import com.nexus.launcher.ui.components.Pill
import com.nexus.launcher.ui.components.RingGauge
import com.nexus.launcher.ui.components.ScreenHeader
import com.nexus.launcher.ui.components.SettingRow
import com.nexus.launcher.ui.theme.Hud
import com.nexus.launcher.ui.theme.Nx

private val PRESETS = listOf(15, 25, 50, 90)

@Composable
fun FocusScreen(graph: NexusGraph, settings: NexusSettings) {
    val context = LocalContext.current
    val focus by graph.focus.state.collectAsStateWithLifecycle()
    val apps by graph.apps.apps.collectAsStateWithLifecycle()
    val usage by graph.apps.usage.collectAsStateWithLifecycle()
    val tick = rememberResumeTick()
    val dndAccess = remember(tick) { graph.focus.hasDndAccess }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    rememberNow() // recompose every second while the timer runs

    val remainingMs = if (focus.active) (focus.endsAt - System.currentTimeMillis()).coerceAtLeast(0) else minutes * 60_000L
    val totalMs = if (focus.active) (focus.endsAt - focus.startedAt).coerceAtLeast(1) else minutes * 60_000L
    val mm = remainingMs / 60_000
    val ss = (remainingMs / 1_000) % 60

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader("Focus mode", if (focus.active) focus.label else "Clear the noise")

        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                RingGauge(remainingMs.toFloat() / totalMs, if (focus.active) Nx.Cyan else Nx.CyanGlow, Modifier.fillMaxSize(), stroke = 8.dp)
                if (focus.active) {
                    // Faint orb behind the countdown; the digits stay readable on top.
                    Box(Modifier.size(220.dp).alpha(0.35f)) {
                        NexusOrb(OrbMode.IDLE, size = 220.dp, reducedMotion = settings.reducedMotion)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(mm, ss),
                        fontFamily = Hud, fontSize = 48.sp, fontWeight = FontWeight.Light,
                        color = Nx.Text,
                    )
                    HudLabel(if (focus.active) "remaining" else "ready", color = if (focus.active) Nx.Cyan else Nx.Muted)
                }
            }
        }

        if (!focus.active) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS.forEach { p ->
                    Pill("$p min", onClick = { minutes = p }, selected = minutes == p, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Pill(
            if (focus.active) "End session" else "Begin $minutes-minute session",
            onClick = {
                if (settings.sounds) Sfx.done()
                if (focus.active) graph.focus.stop() else graph.focus.start(minutes)
            },
            icon = if (focus.active) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
            selected = true,
            accent = if (focus.active) Nx.Amber else Nx.Cyan,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard(Modifier.weight(1f)) {
                HudLabel("Today")
                Text("${focus.todayMinutes} min", fontSize = 26.sp, fontWeight = FontWeight.Light)
            }
            GlassCard(Modifier.weight(1f)) {
                HudLabel("Sessions")
                Text("${focus.todaySessions}", fontSize = 26.sp, fontWeight = FontWeight.Light)
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            SettingRow(
                "Silence notifications",
                if (dndAccess) "Switches Do Not Disturb to priority-only while you focus."
                else "Needs Do Not Disturb access. Tap to allow it.",
                checked = focus.silenceNotifications && dndAccess,
                onChange = { on ->
                    if (!dndAccess) {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    } else {
                        graph.focus.setSilence(on)
                    }
                },
            )
        }

        Spacer(Modifier.height(14.dp))
        HudLabel("Allowed apps · up to 4")
        Text(
            if (focus.allowedPackages.isEmpty()) "None chosen, so every app stays open during focus."
            else "Everything else dims in the orbit until the session ends.",
            style = MaterialTheme.typography.bodyMedium, color = Nx.Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        val candidates = remember(apps, usage) { graph.apps.ranked(apps, usage).take(16) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(candidates, key = { it.component.flattenToString() }) { app ->
                val chosen = app.packageName in focus.allowedPackages
                Column(
                    Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Checkbox) { graph.focus.toggleAllowed(app.packageName) }
                        .semantics { selected = chosen }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppGlyph(app.icon, app.label, size = 48.dp, dimmed = !chosen)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        app.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (chosen) Nx.Cyan else Nx.Muted,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
