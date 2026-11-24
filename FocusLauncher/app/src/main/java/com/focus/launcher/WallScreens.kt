package com.focus.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared frame for the friction screens: centred content that scrolls if it must, and the two
 * choices pinned to the bottom, with going home as the filled, primary one.
 */
@Composable
private fun FocusScaffold(
    primary: String,
    onPrimary: () -> Unit,
    secondary: String?,
    secondaryEnabled: Boolean,
    onSecondary: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
        Button(
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(primary, style = MaterialTheme.typography.labelLarge) }
        if (secondary != null) {
            TextButton(
                onClick = onSecondary,
                enabled = secondaryEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 4.dp, bottom = 8.dp),
            ) { Text(secondary, style = MaterialTheme.typography.labelLarge, color = if (secondaryEnabled) Muted else Muted.copy(alpha = 0.55f)) }
        } else {
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------- wall

/** Everything that isn't essential lives here: no icons, no feed, search first. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WallScreen(
    settings: LauncherSettings,
    apps: List<AppEntry>,
    onOpen: (AppEntry) -> Unit,
    onBack: (skipped: Boolean) -> Unit,
) {
    val context = LocalContext.current
    // You can type straight away; results appear only once the short wait is over.
    // Real tasks lose nothing, browsing loses its momentum.
    val countdown = rememberCountdown(settings.gateSeconds)
    val waiting = !countdown.done
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val distracting = settings.distracting
    val trimmed = query.trim()
    val shown = when {
        waiting -> emptyList()
        trimmed.isNotEmpty() -> apps
            .filter { it.label.contains(trimmed, ignoreCase = true) }
            .sortedBy { if (it.label.startsWith(trimmed, ignoreCase = true)) 0 else 1 }
        settings.wallSearchOnly -> emptyList()
        else -> apps
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp)) {
            IconButton(onClick = { onBack(waiting) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back home", tint = Star)
            }
            Text("Everything else", style = MaterialTheme.typography.titleLarge, color = Star)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Which app do you need?") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Surface1,
                focusedContainerColor = Surface1,
                unfocusedBorderColor = Hairline,
                focusedBorderColor = Muted,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (!waiting) shown.firstOrNull()?.let(onOpen) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focus),
        )

        if (waiting) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
                Text(
                    if (trimmed.isEmpty()) "Name the one app you came for" else "Hold that thought",
                    style = MaterialTheme.typography.titleMedium,
                    color = Star,
                )
                Text(
                    "Results in ${countdown.remaining}s. If nothing comes to mind, you didn't need anything.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else if (shown.isEmpty()) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
                if (trimmed.isEmpty()) {
                    Text("${apps.size} apps wait behind this wall", style = MaterialTheme.typography.titleMedium, color = Star)
                    Text(
                        "Type the one you came for. There's no list to scroll, on purpose.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text("No app called “$trimmed”", style = MaterialTheme.typography.titleMedium, color = Star)
                    Text(
                        "Check the spelling, or try part of the name.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(shown, key = { it.key }) { app ->
                val pauses = app.pkg in distracting
                val doneForToday = pauses && settings.limitReached(app.pkg)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .combinedClickable(
                            role = Role.Button,
                            onClickLabel = "Open ${app.label}",
                            onLongClickLabel = "Options",
                            onClick = { onOpen(app) },
                            onLongClick = { menuFor = app },
                        )
                        .padding(horizontal = 28.dp),
                ) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (pauses) Muted else Star,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    when {
                        doneForToday -> Text("Done for today", style = MaterialTheme.typography.labelMedium, color = Ember)
                        pauses -> Text("Pauses first", style = MaterialTheme.typography.labelMedium, color = Moon)
                    }
                }
            }
        }
        Text(
            "Long-press an app to pin it or change how it opens",
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
        )
    }

    menuFor?.let { app ->
        val pinned = app.key in settings.essentials
        val pauses = app.pkg in settings.distracting
        val locked = settings.strictMode && pauses
        ModalBottomSheet(onDismissRequest = { menuFor = null }, containerColor = Surface1) {
            Text(
                app.label,
                style = MaterialTheme.typography.titleLarge,
                color = Star,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            val itemColors = ListItemDefaults.colors(containerColor = Surface1)
            ListItem(
                headlineContent = { Text(if (pinned) "Remove from home screen" else "Pin to home screen") },
                leadingContent = { Icon(Icons.Filled.Home, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.combinedClickable(role = Role.Button, onClick = {
                    settings.essentials = if (pinned) settings.essentials - app.key else settings.essentials + app.key
                    menuFor = null
                }),
            )
            ListItem(
                headlineContent = {
                    Text(
                        when {
                            locked -> "Pausing is locked by strict mode"
                            pauses -> "Open without a pause"
                            else -> "Pause before opening"
                        }
                    )
                },
                supportingContent = {
                    Text(
                        when {
                            locked -> "Turn strict mode off in Settings to change this."
                            pauses -> "Removes the breathing countdown for this app."
                            else -> "Adds a countdown and a reason before it opens."
                        }
                    )
                },
                leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.combinedClickable(enabled = !locked, role = Role.Button, onClick = {
                    settings.distracting = if (pauses) settings.distracting - app.pkg else settings.distracting + app.pkg
                    menuFor = null
                }),
            )
            ListItem(
                headlineContent = { Text("App info") },
                supportingContent = { Text("Permissions, storage, uninstall") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                colors = itemColors,
                modifier = Modifier.combinedClickable(role = Role.Button, onClick = {
                    AppRepository.openAppInfo(context, app)
                    menuFor = null
                }),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------- pause

private val sessionChoices = listOf(5, 10, 20, 0)

private fun ordinal(n: Int): String = when {
    n % 100 in 11..13 -> "${n}th"
    n % 10 == 1 -> "${n}st"
    n % 10 == 2 -> "${n}nd"
    n % 10 == 3 -> "${n}rd"
    else -> "${n}th"
}

/**
 * The pause in front of shopping, social, video and browser apps. Firm but kind: the cost is shown
 * as stars, the wait grows with each open and at night, and going home is always the easy button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PauseScreen(
    app: AppEntry,
    settings: LauncherSettings,
    onProceed: (sessionMinutes: Int, intention: String?) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val opens = settings.opensToday(app.pkg)
    val limit = settings.dailyOpenLimit
    val night = settings.isNight()
    val countdown = rememberCountdown(settings.pauseFor(app.pkg))
    val reasons = remember(app.kind) { reasonsFor(app.kind) }
    var reason by remember { mutableStateOf<Reason?>(null) }
    var intention by remember { mutableStateOf("") }
    var extended by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(settings.lastSessionMinutes.takeIf { it in sessionChoices } ?: 10) }
    var usedMs by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(app.pkg) {
        usedMs = withContext(Dispatchers.IO) {
            if (Usage.hasAccess(context)) Usage.todayByPackage(context)[app.pkg] ?: 0L else null
        }
    }
    // From the 3rd open on, every purposeful answer has to be named, not just tapped.
    val ask = reason?.let { r -> r.ask ?: if (!r.idle && opens >= 2) "What exactly, this time?" else null }
    val named = intention.trim().takeIf { ask != null && it.isNotEmpty() }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onProceed(session, named)
    }
    fun proceed() {
        settings.lastSessionMinutes = session
        val needsPermission = session > 0 && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else onProceed(session, named)
    }

    val cost = buildList {
        add(if (opens == 0) "First open today" else "This would be the ${ordinal(opens + 1)} open today")
        usedMs?.let { if (it >= 60_000) add("${Usage.format(it)} here already") }
    }.joinToString("  ·  ")

    if (settings.limitReached(app.pkg)) {
        FocusScaffold(primary = "Back home", onPrimary = onCancel, secondary = null, secondaryEnabled = false, onSecondary = {}) {
            Text(app.label, style = MaterialTheme.typography.headlineMedium, color = Star, textAlign = TextAlign.Center)
            StarRow(opens, limit, Modifier.padding(top = 16.dp, bottom = 48.dp))
            Text(
                "All $limit opens used today",
                style = MaterialTheme.typography.headlineSmall,
                color = Ember,
                textAlign = TextAlign.Center,
            )
            Text(
                "It opens again tomorrow. You set this limit on a clearer day; trust that version of you.",
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }

    val intentionMissing = ask != null && intention.trim().length < 3
    val ready = countdown.done && reason != null && !intentionMissing
    FocusScaffold(
        primary = "Not now, take me home",
        onPrimary = onCancel,
        secondary = when {
            !countdown.done -> "Opens when the moon is full"
            reason == null -> "Choose a reason to continue"
            intentionMissing -> "Name it to continue"
            session > 0 -> "Open ${app.label} for $session min"
            else -> "Open ${app.label}"
        },
        secondaryEnabled = ready,
        onSecondary = ::proceed,
    ) {
        Text(app.label, style = MaterialTheme.typography.headlineMedium, color = Star, textAlign = TextAlign.Center)
        StarRow(opens, limit, Modifier.padding(top = 14.dp))
        Text(cost, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        if (night) {
            Text(
                "It's late. Sleep is the better app tonight.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ember,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else if (opens >= 1) {
            Text(
                "The wait grows with each open today.",
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        MoonPhase(countdown, Modifier.padding(top = 12.dp))

        Text(
            "Why are you opening ${app.label}?",
            style = MaterialTheme.typography.titleMedium,
            color = Star,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            reasons.forEach { r ->
                FilterChip(
                    selected = reason == r,
                    onClick = {
                        reason = r
                        // An honest "bored" or "just browsing" earns more time to change your mind.
                        if (r.idle && !extended) {
                            extended = true
                            countdown.add(10)
                        }
                    },
                    label = { Text(r.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Ink,
                        labelColor = Muted,
                        selectedContainerColor = MoonDeep,
                        selectedLabelColor = Moon,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = reason == r,
                        borderColor = Hairline,
                        selectedBorderColor = Moon.copy(alpha = 0.5f),
                    ),
                )
            }
        }
        ask?.let { question ->
            OutlinedTextField(
                value = intention,
                onValueChange = { intention = it.take(60) },
                label = { Text(question) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Hairline, focusedBorderColor = Moon),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Text(
                "Your reminder will tell you what you came for.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
        if (reason?.idle == true) {
            Text(
                idleNudge(app.kind),
                style = MaterialTheme.typography.bodyMedium,
                color = Moon,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (reason != null) {
            Text(
                "Remind me to close it after",
                style = MaterialTheme.typography.titleMedium,
                color = Star,
                modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                sessionChoices.forEach { m ->
                    FilterChip(
                        selected = session == m,
                        onClick = { session = m },
                        label = { Text(if (m == 0) "No reminder" else "$m min") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Ink,
                            labelColor = Muted,
                            selectedContainerColor = Surface3,
                            selectedLabelColor = Star,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = session == m,
                            borderColor = Hairline,
                            selectedBorderColor = Star.copy(alpha = 0.4f),
                        ),
                    )
                }
            }
        }
    }
}
