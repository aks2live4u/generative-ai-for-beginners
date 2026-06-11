package com.prettycountdown.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prettycountdown.data.ThemeMode
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.util.Badge
import com.prettycountdown.util.Badges
import com.prettycountdown.util.UserStats
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/** Theme, defaults for new events, gamification badges and local backup/restore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context))
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val defaultWidgetStyle by viewModel.defaultWidgetStyle.collectAsStateWithLifecycle()
    val defaultCountdownFormat by viewModel.defaultCountdownFormat.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportBackupJson()
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
            }.getOrNull()
            val success = content != null && viewModel.restoreBackupJson(content)
            Toast.makeText(context, if (success) "Backup restored" else "Could not restore backup", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ThemeSection(themeMode, viewModel::setThemeMode) }
            item { DefaultsSection(defaultWidgetStyle, defaultCountdownFormat, viewModel::setDefaultWidgetStyle, viewModel::setDefaultCountdownFormat) }
            item { BadgesSection(userStats) }
            item {
                BackupSection(
                    onExport = { exportLauncher.launch("pretty_countdown_backup.json") },
                    onRestore = { showRestoreConfirm = true },
                )
            }
            item { AboutSection() }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore backup?") },
            text = { Text("This replaces every event, checklist and collection currently on this device with the contents of the backup file.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    restoreLauncher.launch(arrayOf("application/json"))
                }) { Text("Choose File") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSection(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SectionCard("Appearance") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    label = { Text(mode.displayName()) },
                )
            }
        }
    }
}

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultsSection(
    widgetStyle: WidgetStyle,
    countdownFormat: CountdownFormat,
    onWidgetStyleSelect: (WidgetStyle) -> Unit,
    onCountdownFormatSelect: (CountdownFormat) -> Unit,
) {
    SectionCard("Defaults for New Events") {
        Text("Widget style", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetStyle.entries.forEach { style ->
                FilterChip(
                    selected = style == widgetStyle,
                    onClick = { onWidgetStyleSelect(style) },
                    label = { Text("${style.emoji} ${style.displayName}") },
                )
            }
        }
        Text("Countdown format", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CountdownFormat.entries.forEach { format ->
                FilterChip(
                    selected = format == countdownFormat,
                    onClick = { onCountdownFormatSelect(format) },
                    label = { Text(format.displayName) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BadgesSection(stats: UserStats) {
    SectionCard("Badges") {
        Text(
            "${stats.eventsCreated} countdowns created · ${stats.openStreak}-day streak",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Badges.all.forEach { badge -> BadgeChip(badge, stats) }
        }
    }
}

@Composable
private fun BadgeChip(badge: Badge, stats: UserStats) {
    val unlocked = badge.isUnlocked(stats)
    val containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(badge.emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(badge.title, style = MaterialTheme.typography.labelLarge, color = contentColor)
                Text(badge.description, style = MaterialTheme.typography.labelSmall, color = contentColor)
            }
        }
    }
}

@Composable
private fun BackupSection(onExport: () -> Unit, onRestore: () -> Unit) {
    SectionCard("Backup & Restore") {
        Text(
            "Pretty Countdown stores everything on this device only. Export a backup file to keep a copy or move to a new phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export Backup") }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restore Backup") }
    }
}

@Composable
private fun AboutSection() {
    SectionCard("About") {
        Text(
            "Pretty Countdown - beautiful, local-first countdown widgets for life's important moments. No account, no cloud, no ads.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Version 1.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
