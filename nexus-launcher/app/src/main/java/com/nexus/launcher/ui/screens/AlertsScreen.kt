package com.nexus.launcher.ui.screens

import android.content.Intent
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.ai.NexusAIException
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.system.NexusNotification
import com.nexus.launcher.system.NexusNotificationService
import com.nexus.launcher.system.Tier
import com.nexus.launcher.ui.components.GlassCard
import com.nexus.launcher.ui.components.HudLabel
import com.nexus.launcher.ui.components.Pill
import com.nexus.launcher.ui.components.ScreenHeader
import com.nexus.launcher.ui.theme.Nx
import kotlinx.coroutines.launch

@Composable
fun AlertsScreen(graph: NexusGraph, settings: NexusSettings) {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val enabled = remember(tick) { NexusNotificationService.isEnabled(context) }
    val items by NexusNotificationService.items.collectAsStateWithLifecycle()
    val focus by graph.focus.state.collectAsStateWithLifecycle()
    var briefing by rememberSaveable { mutableStateOf<String?>(null) }
    var briefingBusy by remember { mutableStateOf(false) }

    val priority = items.filter { it.tier == Tier.PRIORITY }
    val digest = items.filter { it.tier == Tier.DIGEST }

    fun brief() {
        if (briefingBusy) return
        briefingBusy = true
        graph.scope.launch {
            briefing = try {
                graph.ai.summarizeNotifications(items.map { "${it.appLabel}: ${it.title} — ${it.text.take(140)}" })
                    .also { if (settings.voiceReplies) graph.speaker.speak(it) }
            } catch (e: NexusAIException) {
                e.message
            } catch (e: Exception) {
                "Couldn't build a briefing: ${e.message}"
            }
            briefingBusy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(
            "Notifications",
            when {
                !enabled -> "Signals offline"
                items.isEmpty() -> "All clear"
                else -> "${priority.size} priority · ${digest.size} digest"
            },
        )

        if (!enabled) {
            GlassCard(Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Nx.Cyan)
                Spacer(Modifier.height(10.dp))
                Text("Let NEXUS read your notifications", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "NEXUS sorts them into Priority (people, calls, reminders) and a quiet Digest, and can brief you aloud. " +
                        "Nothing is sent anywhere unless you ask for a briefing.",
                    style = MaterialTheme.typography.bodyMedium, color = Nx.Muted,
                )
                Spacer(Modifier.height(14.dp))
                Pill("Grant notification access", onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                })
            }
            return@Column
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (focus.active) {
                item {
                    GlassCard(Modifier.fillMaxWidth(), accent = Nx.Amber) {
                        HudLabel("Focus Mode active", color = Nx.Amber)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (graph.focus.hasDndAccess && focus.silenceNotifications) "Only priority interruptions can reach you until the session ends."
                            else "Notifications still ring. Allow Do Not Disturb access in Focus to silence them.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (items.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth(), onClick = { brief() }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Nx.Cyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            HudLabel(if (briefingBusy) "NEXUS is reading…" else "Brief me", color = Nx.Cyan)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            briefing ?: "Tap for a short spoken summary of what needs you and what can wait.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (briefing == null) Nx.Muted else Nx.Text,
                        )
                    }
                }
            }
            if (priority.isNotEmpty()) {
                item { HudLabel("Priority", color = Nx.Amber, modifier = Modifier.padding(top = 8.dp)) }
                items(priority, key = { it.key }) { NotificationCard(it, accentAmber = true) }
            }
            if (digest.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HudLabel("Digest", Modifier.weight(1f))
                        Pill("Clear digest", onClick = { NexusNotificationService.dismissAll(digest.map { it.key }) })
                    }
                }
                items(digest, key = { it.key }) { NotificationCard(it, accentAmber = false) }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        "Nothing waiting. Enjoy the quiet.",
                        style = MaterialTheme.typography.bodyLarge, color = Nx.Muted,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun NotificationCard(n: NexusNotification, accentAmber: Boolean) {
    GlassCard(
        Modifier.fillMaxWidth(),
        accent = if (accentAmber) Nx.Amber else Nx.Cyan,
        onClick = { runCatching { n.contentIntent?.send() } },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                HudLabel("${n.appLabel} · ${DateUtils.getRelativeTimeSpanString(n.postTime)}")
                Spacer(Modifier.height(4.dp))
                if (n.title.isNotBlank()) Text(n.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (n.text.isNotBlank()) Text(n.text, style = MaterialTheme.typography.bodyMedium, color = Nx.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { NexusNotificationService.dismiss(n.key) }) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = Nx.Muted)
            }
        }
    }
}
