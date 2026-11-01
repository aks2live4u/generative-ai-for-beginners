package com.focus.launcher

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_ESSENTIALS = 30

private val textColors = listOf(
    0xFFE8E2D6, 0xFFFFFFFF, 0xFFB8B8B8, 0xFFA8C3A0, 0xFF9CC4E4, 0xFFE8A9A0, 0xFFF0C674, 0xFF111111,
).map { Color(it) }

private val backgrounds = listOf(
    0xFF000000 to 0xFF000000, // pure black (easy on OLED and on the eyes)
    0xFF0F2027 to 0xFF2C5364, // ocean
    0xFF1D2B1F to 0xFF4A6B4F, // forest
    0xFF2B1B3D to 0xFF6B4C7A, // dusk
    0xFF3A2618 to 0xFF8C5A3C, // clay
    0xFF1C1C1E to 0xFF3A3A3C, // graphite
    0xFFF3EEE6 to 0xFFE3D9C9, // paper (pair with a dark text colour)
).map { Color(it.first) to Color(it.second) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    apps: List<AppEntry>,
    isDefault: Boolean,
    onMakeDefault: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickEssentials by remember { mutableStateOf(false) }
    var pickDistracting by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { Media.copy(context, uri, Media.wallpaperFile(context)) }
            if (ok) {
                settings.wallpaperVersion += 1
                settings.wallpaperMode = WallpaperMode.PHOTO
            }
        }
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                Media.copy(context, uri, Media.fontFile(context)) && Media.loadFont(context) != null
            }
            if (ok) {
                settings.fontVersion += 1
                settings.fontChoice = FontChoice.CUSTOM
            }
            Toast.makeText(context, if (ok) "Font imported" else "That file isn't a usable .ttf/.otf font", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", color = Cream, fontSize = 28.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Done") }
        }

        if (isDefault) {
            Text("✓ Focus Launcher is your default home screen", color = Sage, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = { DefaultLauncher.openHomeSettings(context) }) { Text("Change default home app") }
        } else {
            Button(onClick = onMakeDefault, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Make Focus Launcher my default home screen")
            }
            Text(
                "Until it's the default, pressing Home goes back to your old launcher with all its icons.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // ---------------- Essentials ----------------
        Section("Essential apps", "Only these appear on your home screen. Everything else goes behind the wall.")
        val essentialApps = settings.essentials.mapNotNull { key -> apps.firstOrNull { it.key == key } }
        essentialApps.forEachIndexed { index, app ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(app.label, color = Cream, fontSize = 18.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { settings.essentials = settings.essentials.move(index, -1) }, enabled = index > 0) { Text("↑") }
                TextButton(onClick = { settings.essentials = settings.essentials.move(index, 1) }, enabled = index < essentialApps.size - 1) { Text("↓") }
                TextButton(onClick = { settings.essentials = settings.essentials - app.key }) { Text("✕") }
            }
        }
        OutlinedButton(onClick = { pickEssentials = true }) { Text("Choose essential apps") }
        Text("Layout on home screen", color = Cream, modifier = Modifier.padding(top = 12.dp))
        Chips(listOf(1, 2), settings.homeColumns, { if (it == 1) "1 column" else "2 columns" }) { settings.homeColumns = it }
        LabeledSlider(
            label = "App name size: ${(settings.appTextScale * 100).toInt()}%",
            value = settings.appTextScale,
            range = 0.6f..1.4f,
        ) { settings.appTextScale = it }

        // ---------------- Distracting ----------------
        Section(
            "Distracting apps",
            "These get a breathing pause, a reason prompt and today's count before they open. " +
                "Shopping, social, video and browsers are detected automatically.",
        )
        val distractingApps = apps.filter { it.pkg in settings.distracting }.distinctBy { it.pkg }
        Text(
            if (distractingApps.isEmpty()) "None yet" else distractingApps.joinToString(", ") { it.label },
            color = Muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { pickDistracting = true }) { Text("Edit list") }
            OutlinedButton(onClick = {
                val detected = apps.filter { it.kind.isDistracting }.map { it.pkg }
                settings.distracting = settings.distracting + detected
                Toast.makeText(context, "Found ${detected.distinct().size} distracting apps", Toast.LENGTH_SHORT).show()
            }) { Text("Auto-detect") }
        }

        // ---------------- Friction ----------------
        Section("Friction", "A little friction is what breaks the autopilot.")
        LabeledSlider(
            label = "Wait before opening the wall: ${settings.gateSeconds}s",
            value = settings.gateSeconds.toFloat(),
            range = 0f..30f,
            steps = 29,
        ) { settings.gateSeconds = it.toInt() }
        LabeledSlider(
            label = "Pause before distracting apps: ${settings.pauseSeconds}s",
            value = settings.pauseSeconds.toFloat(),
            range = 5f..60f,
            steps = 10,
        ) { settings.pauseSeconds = it.toInt() }
        ToggleRow("Search-only wall (no scrolling list)", settings.wallSearchOnly) { settings.wallSearchOnly = it }

        // ---------------- Wallpaper ----------------
        Section("Wallpaper")
        Chips(WallpaperMode.entries, settings.wallpaperMode, { it.label }) { settings.wallpaperMode = it }
        when (settings.wallpaperMode) {
            WallpaperMode.PHOTO -> OutlinedButton(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Pick a photo") }
            WallpaperMode.COLOR -> FlowRow(Modifier.padding(top = 8.dp)) {
                backgrounds.forEach { (top, bottom) ->
                    Swatch(
                        listOf(top, bottom),
                        selected = settings.wallpaperColor == top.toArgb() && settings.wallpaperColor2 == bottom.toArgb(),
                    ) {
                        settings.wallpaperColor = top.toArgb()
                        settings.wallpaperColor2 = bottom.toArgb()
                    }
                }
            }
            WallpaperMode.SYSTEM -> Text("Uses your phone's current wallpaper.", color = Muted, modifier = Modifier.padding(top = 8.dp))
        }
        LabeledSlider(
            label = "Dim: ${(settings.dim * 100).toInt()}%",
            value = settings.dim,
            range = 0f..0.8f,
        ) { settings.dim = it }

        // ---------------- Clock & text ----------------
        Section("Clock")
        Chips(ClockStyle.entries, settings.clockStyle, { it.label }) { settings.clockStyle = it }
        LabeledSlider(
            label = "Clock size: ${(settings.clockScale * 100).toInt()}%",
            value = settings.clockScale,
            range = 0.6f..1.6f,
        ) { settings.clockScale = it }
        ToggleRow("24-hour time", settings.use24h) { settings.use24h = it }
        ToggleRow("Show date", settings.showDate) { settings.showDate = it }

        Section("Font")
        Chips(FontChoice.entries.filter { it != FontChoice.CUSTOM || Media.fontFile(context).exists() }, settings.fontChoice, { it.label }) {
            settings.fontChoice = it
        }
        OutlinedButton(onClick = { fontPicker.launch(arrayOf("*/*")) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Import a font (.ttf / .otf)")
        }

        Section("Text colour")
        FlowRow {
            textColors.forEach { c ->
                Swatch(listOf(c), selected = settings.textColor == c.toArgb()) { settings.textColor = c.toArgb() }
            }
        }

        Section("Home note", "A line under the clock — an intention, a reminder, a mantra.")
        OutlinedTextField(
            value = settings.homeNote,
            onValueChange = { settings.homeNote = it.take(80) },
            placeholder = { Text("e.g. Read, don't scroll.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // ---------------- Screen time ----------------
        Section("Screen time", "Shows today's total under the clock and per-app time on the pause screen.")
        ToggleRow("Show screen time", settings.showScreenTime) { settings.showScreenTime = it }
        if (!Usage.hasAccess(context)) {
            OutlinedButton(onClick = { Usage.openAccessSettings(context) }) { Text("Grant usage access") }
        } else {
            Text("Usage access granted ✓", color = Sage)
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "Tip: swipe up on the home screen for the wall, long-press for these settings.",
            color = Muted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (pickEssentials) {
        AppPickerDialog(
            title = "Essential apps",
            apps = apps,
            initiallySelected = settings.essentials.toSet(),
            keyOf = { it.key },
            maxSelected = MAX_ESSENTIALS,
            onDismiss = { pickEssentials = false },
            onDone = { chosen ->
                // keep the existing order, append new picks at the end
                settings.essentials = settings.essentials.filter { it in chosen } + chosen.filter { it !in settings.essentials }
                pickEssentials = false
            },
        )
    }
    if (pickDistracting) {
        AppPickerDialog(
            title = "Distracting apps",
            apps = apps,
            initiallySelected = settings.distracting,
            keyOf = { it.pkg },
            onDismiss = { pickDistracting = false },
            onDone = { chosen ->
                settings.distracting = chosen
                pickDistracting = false
            },
        )
    }
}

private fun List<String>.move(index: Int, delta: Int): List<String> {
    val target = index + delta
    if (target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}

@Composable
private fun Section(title: String, subtitle: String? = null) {
    Spacer(Modifier.height(28.dp))
    HorizontalDivider(color = Color(0x22FFFFFF))
    Spacer(Modifier.height(16.dp))
    Text(title, color = Cream, style = MaterialTheme.typography.titleMedium)
    if (subtitle != null) {
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
    } else {
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Cream, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(label, color = Cream)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}
