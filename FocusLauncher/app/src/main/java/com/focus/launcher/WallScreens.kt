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

// ---------------------------------------------------------------- gate

/** A short pause before the wall even opens: name what you need first. */
@Composable
fun GateScreen(seconds: Int, onContinue: () -> Unit, onBack: () -> Unit) {
    val countdown = rememberCountdown(seconds)
    FocusScaffold(
        primary = "Nothing, take me home",
        onPrimary = onBack,
        secondary = if (countdown.done) "Open the wall" else "The wall opens in ${countdown.remaining}s",
        secondaryEnabled = countdown.done,
        onSecondary = onContinue,
    ) {
        Text("What do you need?", style = MaterialTheme.typography.headlineMedium, color = Cream, textAlign = TextAlign.Center)
        Text(
            "Have one app in mind before you go in.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
        )
        BreathingCircle(countdown.remaining)
    }
}

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
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val distracting = settings.distracting
    val trimmed = query.trim()
    val shown = when {
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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back home", tint = Cream)
            }
            Text("Everything else", style = MaterialTheme.typography.titleLarge, color = Cream)
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
            keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let(onOpen) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focus),
        )

        if (shown.isEmpty()) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
                if (trimmed.isEmpty()) {
                    Text("${apps.size} apps wait behind this wall", style = MaterialTheme.typography.titleMedium, color = Cream)
                    Text(
                        "Type the one you came for. There's no list to scroll, on purpose.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text("No app called “$trimmed”", style = MaterialTheme.typography.titleMedium, color = Cream)
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
                        color = if (pauses) Muted else Cream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    when {
                        doneForToday -> Text("Done for today", style = MaterialTheme.typography.labelMedium, color = Rose)
                        pauses -> Text("Pauses first", style = MaterialTheme.typography.labelMedium, color = Sage)
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
                color = Cream,
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

private val reasons = listOf("Something specific", "Replying to someone", "Bored", "Habit")
private val idleReasons = setOf("Bored", "Habit")
private val sessionChoices = listOf(5, 10, 20, 0)

/** The mindful pause in front of shopping, social, video and browser apps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PauseScreen(
    app: AppEntry,
    settings: LauncherSettings,
    onProceed: (sessionMinutes: Int) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val countdown = rememberCountdown(settings.pauseSeconds)
    var reason by remember { mutableStateOf<String?>(null) }
    var extended by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(settings.lastSessionMinutes.takeIf { it in sessionChoices } ?: 10) }
    var usedMs by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(app.pkg) {
        usedMs = withContext(Dispatchers.IO) {
            if (Usage.hasAccess(context)) Usage.todayByPackage(context)[app.pkg] ?: 0L else null
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onProceed(session)
    }
    fun proceed() {
        settings.lastSessionMinutes = session
        val needsPermission = session > 0 && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else onProceed(session)
    }

    val opens = settings.opensToday(app.pkg)
    val limit = settings.dailyOpenLimit
    val facts = buildList {
        add(
            when {
                limit > 0 -> "$opens of $limit opens today"
                opens == 0 -> "Not opened yet today"
                opens == 1 -> "Opened once today"
                else -> "Opened $opens times today"
            }
        )
        usedMs?.let { if (it >= 60_000) add("${Usage.format(it)} so far") }
    }.joinToString("  ·  ")

    if (settings.limitReached(app.pkg)) {
        FocusScaffold(primary = "Back home", onPrimary = onCancel, secondary = null, secondaryEnabled = false, onSecondary = {}) {
            Text(app.label, style = MaterialTheme.typography.headlineMedium, color = Cream, textAlign = TextAlign.Center)
            Text(facts, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 6.dp, bottom = 48.dp))
            Text(
                "That's all $limit for today",
                style = MaterialTheme.typography.headlineSmall,
                color = Rose,
                textAlign = TextAlign.Center,
            )
            Text(
                "It opens again tomorrow. You set this limit for a reason.",
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }

    val ready = countdown.done && reason != null
    FocusScaffold(
        primary = "Not now, take me home",
        onPrimary = onCancel,
        secondary = when {
            !countdown.done -> "Wait ${countdown.remaining}s"
            reason == null -> "Choose a reason to continue"
            session > 0 -> "Open ${app.label} for $session min"
            else -> "Open ${app.label}"
        },
        secondaryEnabled = ready,
        onSecondary = ::proceed,
    ) {
        Text(app.label, style = MaterialTheme.typography.headlineMedium, color = Cream, textAlign = TextAlign.Center)
        Text(facts, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        BreathingCircle(countdown.remaining)

        Text(
            "Why are you opening it?",
            style = MaterialTheme.typography.titleMedium,
            color = Cream,
            modifier = Modifier.padding(top = 32.dp, bottom = 10.dp),
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
                        // An honest "bored" or "habit" earns ten more seconds to change your mind.
                        if (r in idleReasons && !extended) {
                            extended = true
                            countdown.add(10)
                        }
                    },
                    label = { Text(r) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SageDeep, selectedLabelColor = Sage),
                )
            }
        }
        if (reason in idleReasons) {
            Text(
                "Totally human. Ten more seconds to see if you still want it.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sage,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (reason != null) {
            Text(
                "Remind me to close it after",
                style = MaterialTheme.typography.titleMedium,
                color = Cream,
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
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Surface3, selectedLabelColor = Cream),
                    )
                }
            }
        }
    }
}
