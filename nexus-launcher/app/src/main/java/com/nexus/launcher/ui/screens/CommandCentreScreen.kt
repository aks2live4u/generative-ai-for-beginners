package com.nexus.launcher.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.ai.NexusAIException
import com.nexus.launcher.ai.NexusAction
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.system.NexusNotificationService
import com.nexus.launcher.system.Sfx
import com.nexus.launcher.system.Tier
import com.nexus.launcher.system.VoiceInput
import com.nexus.launcher.system.rememberBattery
import com.nexus.launcher.ui.components.HudLabel
import com.nexus.launcher.ui.components.NexusOrb
import com.nexus.launcher.ui.components.OrbMode
import com.nexus.launcher.ui.components.Pill
import com.nexus.launcher.ui.theme.Nx
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Exchange(val fromUser: Boolean, val text: String, val isError: Boolean = false)

/** Conversation state that survives switching screens. */
class CommandSession {
    val log = mutableStateListOf<Exchange>()
    var thinking by mutableStateOf(false)
    var pendingPrompt: String? by mutableStateOf(null)
}

private val QUICK = listOf(
    "What's the weather?",
    "Focus for 25 minutes",
    "Summarise my notifications",
    "Set a 10 minute timer",
    "Open the camera",
)

@Composable
fun CommandCentreScreen(
    graph: NexusGraph,
    settings: NexusSettings,
    session: CommandSession,
    runAction: (NexusAction) -> Unit,
) {
    val context = LocalContext.current
    val voice = remember { VoiceInput(context) }
    DisposableEffect(Unit) { onDispose { voice.release() } }
    val listen by voice.state.collectAsStateWithLifecycle()
    val speaking by graph.speaker.speaking.collectAsStateWithLifecycle()
    val focus by graph.focus.state.collectAsStateWithLifecycle()
    val notifications by NexusNotificationService.items.collectAsStateWithLifecycle()
    val apps by graph.apps.apps.collectAsStateWithLifecycle()
    val battery = rememberBattery()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun deviceContext(): String = buildString {
        append("time ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE d MMM HH:mm")))
        append("; battery ").append(battery.percent).append("%").append(if (battery.charging) " charging" else "")
        append("; focus ").append(if (focus.active) "on (${focus.label})" else "off")
        append("; notifications ").append(notifications.count { it.tier == Tier.PRIORITY }).append(" priority, ")
            .append(notifications.size).append(" total")
    }

    fun send(raw: String) {
        val prompt = raw.trim()
        if (prompt.isEmpty() || session.thinking) return
        input = ""
        session.log += Exchange(true, prompt)
        session.thinking = true
        graph.speaker.stop()
        // App-wide scope so a reply still lands if you switch screens while NEXUS is thinking.
        graph.scope.launch {
            try {
                val wantsSummary = prompt.contains("notification", true) &&
                    (prompt.contains("summar", true) || prompt.contains("brief", true))
                if (wantsSummary && notifications.isNotEmpty()) {
                    val lines = notifications.map { "${it.appLabel}: ${it.title} — ${it.text.take(140)}" }
                    val text = graph.ai.summarizeNotifications(lines)
                    session.log += Exchange(false, text)
                    if (settings.voiceReplies) graph.speaker.speak(text)
                } else {
                    val reply = graph.ai.ask(prompt, deviceContext(), apps.map { it.label }.distinct())
                    val text = reply.text.ifBlank { "Done." }
                    session.log += Exchange(false, text)
                    if (settings.voiceReplies) graph.speaker.speak(text)
                    reply.actions.forEach(runAction)
                }
                if (settings.sounds) Sfx.done()
            } catch (e: NexusAIException) {
                session.log += Exchange(false, e.message ?: "Something went wrong.", isError = true)
            } catch (e: Exception) {
                session.log += Exchange(false, "Something went wrong: ${e.message}", isError = true)
            } finally {
                session.thinking = false
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (settings.sounds) Sfx.listen()
            voice.start(::send)
        } else {
            session.log += Exchange(false, "Microphone access is off, so type to me instead.", isError = true)
        }
    }

    fun toggleMic() {
        if (listen.listening) { voice.stop(); return }
        graph.speaker.stop()
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (settings.sounds) Sfx.listen()
            voice.start(::send)
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // A prompt handed over from Home: send it, or open the mic if it's empty.
    LaunchedEffect(session.pendingPrompt) {
        val p = session.pendingPrompt ?: return@LaunchedEffect
        session.pendingPrompt = null
        if (p.isBlank()) toggleMic() else send(p)
    }
    LaunchedEffect(session.log.size) {
        if (session.log.isNotEmpty()) listState.animateScrollToItem(session.log.size - 1)
    }

    val mode = when {
        listen.listening -> OrbMode.LISTENING
        session.thinking -> OrbMode.THINKING
        speaking -> OrbMode.SPEAKING
        else -> OrbMode.IDLE
    }
    val status = when (mode) {
        OrbMode.LISTENING -> listen.partial.ifBlank { "Listening…" }
        OrbMode.THINKING -> "Thinking…"
        OrbMode.SPEAKING -> "Speaking"
        OrbMode.IDLE -> listen.error ?: if (settings.apiKey.isBlank()) "Add an API key in Settings to wake me" else "Tap the core or say a command"
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HudLabel("AI Command Centre", Modifier.weight(1f), color = Nx.Cyan)
            if (speaking) {
                IconButton(onClick = { graph.speaker.stop() }) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeOff, contentDescription = "Stop speaking", tint = Nx.Muted)
                }
            }
            if (session.log.isNotEmpty()) {
                IconButton(onClick = { session.log.clear(); graph.ai.reset() }) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear conversation", tint = Nx.Muted)
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { toggleMic() }
                    .semantics { contentDescription = if (listen.listening) "Stop listening" else "Talk to NEXUS" },
            ) {
                NexusOrb(
                    mode = mode,
                    size = if (session.log.isEmpty()) 240.dp else 150.dp,
                    level = listen.level,
                    reducedMotion = settings.reducedMotion,
                )
            }
        }
        Text(
            status,
            style = MaterialTheme.typography.bodyLarge,
            color = if (listen.error != null && mode == OrbMode.IDLE) Nx.Amber else Nx.CyanSoft,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
            maxLines = 2,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (session.log.isEmpty()) {
                item {
                    Text(
                        "Ask anything, or tell me what to do: open apps, set alarms and timers, start Focus Mode, brief you on your notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Nx.Muted,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            items(session.log) { ex -> Bubble(ex) }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(QUICK) { q -> Pill(q, onClick = { send(q) }) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Command NEXUS…", color = Nx.Muted) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(input) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Nx.Cyan, unfocusedBorderColor = Nx.Line,
                    focusedContainerColor = Nx.Surface, unfocusedContainerColor = Nx.Surface,
                    cursorColor = Nx.Cyan,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            val micActive = listen.listening
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (micActive) Nx.Cyan else Nx.SurfaceHi)
                    .border(1.dp, Nx.Line, CircleShape)
                    .clickable(role = Role.Button) { if (input.isNotBlank()) send(input) else toggleMic() },
                contentAlignment = Alignment.Center,
            ) {
                val icon = when {
                    input.isNotBlank() -> Icons.AutoMirrored.Outlined.Send
                    micActive -> Icons.Outlined.Stop
                    else -> Icons.Outlined.Mic
                }
                val label = when {
                    input.isNotBlank() -> "Send"
                    micActive -> "Stop listening"
                    else -> "Speak"
                }
                Icon(icon, contentDescription = label, tint = if (micActive) Nx.Obsidian else Nx.Cyan)
            }
        }
    }
}

@Composable
private fun Bubble(ex: Exchange) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (ex.fromUser) Arrangement.End else Arrangement.Start) {
        val shape = RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = if (ex.fromUser) 20.dp else 6.dp, bottomEnd = if (ex.fromUser) 6.dp else 20.dp,
        )
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (ex.fromUser) Nx.SurfaceHi else Nx.Surface)
                .border(1.dp, if (ex.isError) Nx.Amber.copy(alpha = 0.5f) else if (ex.fromUser) Nx.Line else Nx.Cyan.copy(alpha = 0.35f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (!ex.fromUser) {
                HudLabel(if (ex.isError) "System" else "NEXUS", color = if (ex.isError) Nx.Amber else Nx.Cyan)
                Spacer(Modifier.height(4.dp))
            }
            Text(ex.text, style = MaterialTheme.typography.bodyLarge, color = if (ex.fromUser) Nx.Text.copy(alpha = 0.9f) else Nx.Text)
        }
    }
}
