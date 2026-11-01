package com.focus.launcher

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** A slowly breathing circle — the visual heart of every friction screen. */
@Composable
fun BreathingCircle(label: String) {
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale",
    )
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(180.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Sage.copy(alpha = 0.18f))
        )
        Text(label, color = Cream, fontSize = 40.sp)
    }
}

@Composable
private fun rememberCountdown(seconds: Int): Int {
    var remaining by remember { mutableIntStateOf(seconds) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }
    return remaining
}

/** Short pause before the hidden wall even opens. */
@Composable
fun GateScreen(seconds: Int, onContinue: () -> Unit, onBack: () -> Unit) {
    val remaining = rememberCountdown(seconds)
    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("What do you need?", color = Cream, fontSize = 28.sp)
        Text(
            "Have a specific app in mind before you go in.",
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 36.dp),
        )
        BreathingCircle(if (remaining > 0) remaining.toString() else "✓")
        Spacer(Modifier.height(48.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Nothing really — go home") }
        TextButton(onClick = onContinue, enabled = remaining == 0, modifier = Modifier.padding(top = 8.dp)) {
            Text(if (remaining > 0) "Wait ${remaining}s" else "Yes, I need something")
        }
    }
}

/** Everything that isn't essential lives here: no icons, search first. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallScreen(
    settings: LauncherSettings,
    apps: List<AppEntry>,
    onOpen: (AppEntry) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val distracting = settings.distracting
    val shown = when {
        query.isNotBlank() -> apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        settings.wallSearchOnly -> emptyList()
        else -> apps
    }

    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Everything else", color = Cream, fontSize = 24.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Home") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Type the app you need") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let(onOpen) }),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(focus),
        )
        if (shown.isEmpty()) {
            Text(
                if (query.isBlank()) "${apps.size} apps are behind this wall.\nSearch for the one you came for — no browsing."
                else "No app matches \"$query\"",
                color = Muted,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(shown, key = { it.key }) { app ->
                val isDistracting = app.pkg in distracting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onOpen(app) }, onLongClick = { menuFor = app })
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        app.label,
                        color = if (isDistracting) Muted else Cream,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDistracting) {
                        Text("mindful", color = Sage, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text("Long-press an app to pin it or mark it distracting", color = Muted, fontSize = 12.sp)
    }

    menuFor?.let { app ->
        val pinned = app.key in settings.essentials
        val isDistracting = app.pkg in settings.distracting
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(app.label) },
            text = {
                Column {
                    TextButton(onClick = {
                        settings.essentials = if (pinned) settings.essentials - app.key else settings.essentials + app.key
                        menuFor = null
                    }) { Text(if (pinned) "Remove from home screen" else "Pin to home screen") }
                    TextButton(onClick = {
                        settings.distracting = if (isDistracting) settings.distracting - app.pkg else settings.distracting + app.pkg
                        menuFor = null
                    }) { Text(if (isDistracting) "Stop pausing before open" else "Mark as distracting (pause first)") }
                    TextButton(onClick = {
                        AppRepository.openAppInfo(context, app)
                        menuFor = null
                    }) { Text("App info / uninstall") }
                }
            },
            confirmButton = { TextButton(onClick = { menuFor = null }) { Text("Close") } },
        )
    }
}

private val reasons = listOf(
    "I need something specific",
    "Replying to someone",
    "Just bored",
    "Habit / autopilot",
)

/** The mindful pause in front of shopping, social, video and browser apps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PauseScreen(
    app: AppEntry,
    settings: LauncherSettings,
    onProceed: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val remaining = rememberCountdown(settings.pauseSeconds)
    var reason by remember { mutableStateOf<String?>(null) }
    var usedMs by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(app.pkg) {
        usedMs = withContext(Dispatchers.IO) {
            if (Usage.hasAccess(context)) Usage.todayByPackage(context)[app.pkg] ?: 0L else null
        }
    }
    val opens = settings.opensToday(app.pkg)
    val idle = reason == reasons[2] || reason == reasons[3]

    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Opening ${app.label}", color = Cream, fontSize = 26.sp, textAlign = TextAlign.Center)
        val facts = buildList {
            add(if (opens == 0) "First time today" else "Opened $opens time${if (opens == 1) "" else "s"} today")
            usedMs?.let { add("${Usage.format(it)} used today") }
        }
        Text(facts.joinToString("  ·  "), color = Muted, modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))

        BreathingCircle(if (remaining > 0) remaining.toString() else "✓")
        Text("Breathe in… and out.", color = Muted, modifier = Modifier.padding(top = 16.dp, bottom = 20.dp))

        Text("Why are you opening it?", color = Cream)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            reasons.forEach { r ->
                FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r) })
            }
        }
        if (idle) {
            Text(
                "Totally human. A short walk or a glass of water might feel better than a scroll.",
                color = Sage,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Not now — take me home") }
        TextButton(
            onClick = onProceed,
            enabled = remaining == 0 && reason != null,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                when {
                    remaining > 0 -> "Wait ${remaining}s"
                    reason == null -> "Pick a reason to continue"
                    else -> "Open ${app.label}"
                },
                color = if (remaining == 0 && reason != null) Muted else Color.Unspecified,
            )
        }
    }
}
