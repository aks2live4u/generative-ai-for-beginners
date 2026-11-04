package com.focus.launcher

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_ESSENTIALS = 30
private const val STRICT_UNLOCK_SECONDS = 60

private val textColors = listOf(
    0xFFECE6DA to "Cream", 0xFFFFFFFF to "White", 0xFFBDB8AE to "Stone", 0xFFA9C4A1 to "Sage",
    0xFF9EC3E0 to "Sky", 0xFFE6A99E to "Rose", 0xFFEDC77A to "Amber", 0xFF151514 to "Ink",
).map { Color(it.first) to it.second }

private val backgrounds = listOf(
    Triple(0xFF000000, 0xFF000000, "Black"),
    Triple(0xFF0E1A1F, 0xFF2B4C59, "Ocean"),
    Triple(0xFF141F16, 0xFF3F5E44, "Forest"),
    Triple(0xFF1E1529, 0xFF5E4570, "Dusk"),
    Triple(0xFF2A1A10, 0xFF7E5236, "Clay"),
    Triple(0xFF161616, 0xFF3A3A3C, "Graphite"),
    Triple(0xFFF3EEE5, 0xFFE2D8C7, "Paper"),
    Triple(0xFFE9EEE6, 0xFFCFDCCB, "Mist"),
).map { Triple(Color(it.first), Color(it.second), it.third) }

private val openLimits = listOf(0, 3, 5, 10, 20)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var arrange by remember { mutableStateOf(false) }

    // Strict mode: friction can only be loosened after waiting a minute, once per visit.
    var unlockedThisVisit by remember { mutableStateOf(!settings.strictMode) }
    var unlockRemaining by remember { mutableIntStateOf(0) }
    if (unlockRemaining > 0) {
        LaunchedEffect(Unit) {
            while (unlockRemaining > 0) {
                delay(1000)
                unlockRemaining--
            }
            unlockedThisVisit = true
        }
    }
    val frictionLocked = settings.strictMode && !unlockedThisVisit

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { Media.copy(context, uri, Media.wallpaperFile(context)) }
            if (ok) {
                settings.wallpaperVersion += 1
                settings.wallpaperMode = WallpaperMode.PHOTO
            } else {
                Toast.makeText(context, "Couldn't read that photo. Try another one.", Toast.LENGTH_SHORT).show()
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
                settings.clockFont = FontChoice.CUSTOM
            }
            Toast.makeText(
                context,
                if (ok) "Font imported and set as the clock font" else "That file isn't a .ttf or .otf font",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Cream, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Done") }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            if (!isDefault) {
                Callout(
                    title = "Not your home screen yet",
                    body = "Until it is, the Home button takes you back to your old launcher and all its icons.",
                ) {
                    Button(onClick = onMakeDefault) { Text("Make it my home screen") }
                }
            }

            // ------------------------------------------------ home screen
            SectionTitle("Home screen", "Only these apps sit on your home screen.")
            val essentialApps = settings.essentials.mapNotNull { key -> apps.firstOrNull { it.key == key } }
            Text(
                when (essentialApps.size) {
                    0 -> "No apps yet"
                    1 -> "1 app: ${essentialApps.first().label}"
                    else -> "${essentialApps.size} apps: " + essentialApps.take(4).joinToString(", ") { it.label } +
                        if (essentialApps.size > 4) " and ${essentialApps.size - 4} more" else ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                FilledTonalButton(onClick = { pickEssentials = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (essentialApps.isEmpty()) "Choose apps" else "Add or remove")
                }
                if (essentialApps.size > 1) {
                    OutlinedButton(onClick = { arrange = true }) { Text("Change order") }
                }
            }

            Label("Layout")
            Segmented(listOf(1, 2), settings.homeColumns, { if (it == 1) "One column" else "Two columns" }) { settings.homeColumns = it }
            LabeledSlider("Text size", "${(settings.appTextScale * 100).toInt()}%", settings.appTextScale, 0.7f..1.8f) {
                settings.appTextScale = it
            }
            Label("Note under the clock")
            OutlinedTextField(
                value = settings.homeNote,
                onValueChange = { settings.homeNote = it.take(80) },
                placeholder = { Text("An intention, e.g. Read, don't scroll") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Hairline),
                modifier = Modifier.fillMaxWidth(),
            )

            // ------------------------------------------------ look
            SectionTitle("Look", "Changes show here first, then on your home screen.")
            HomePreview(settings, apps)

            Label("Clock style")
            Chips(ClockStyle.entries, settings.clockStyle, { it.label }) { settings.clockStyle = it }

            Label("Clock font")
            val clockFonts = FontChoice.entries.filter { it != FontChoice.CUSTOM || Media.fontFile(context).exists() }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(clockFonts, key = { it.name }) { f ->
                    FontTile(f, "12:45", settings.clockWeight, settings.fontVersion, f == settings.clockFont) {
                        settings.clockFont = f
                    }
                }
            }
            TextButton(onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream", "*/*")) }) {
                Text("Import your own font (.ttf, .otf)")
            }
            val clockFont = settings.clockFont
            if (clockFont.hasWeights) {
                Label("Clock weight")
                val steps = FontWeightSteps.filter { it.first in clockFont.minWeight..clockFont.maxWeight }
                val current = steps.minByOrNull { kotlin.math.abs(it.first - clockFont.clamp(settings.clockWeight)) }?.first
                Chips(steps.map { it.first }, current ?: 400, { w -> steps.first { it.first == w }.second }) { settings.clockWeight = it }
            }
            LabeledSlider("Clock size", "${(settings.clockScale * 100).toInt()}%", settings.clockScale, 0.6f..1.5f) {
                settings.clockScale = it
            }
            ToggleRow("24-hour time", checked = settings.use24h) { settings.use24h = it }
            ToggleRow("Show the date", checked = settings.showDate) { settings.showDate = it }

            Label("Text font")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(FontChoice.entries.filter { it != FontChoice.CUSTOM }, key = { it.name }) { f ->
                    FontTile(f, "Aa", 400, settings.fontVersion, f == settings.textFont) { settings.textFont = f }
                }
            }

            Label("Text colour")
            FlowRow {
                textColors.forEach { (c, name) ->
                    Swatch(listOf(c), selected = settings.textColor == c.toArgb(), description = name) { settings.textColor = c.toArgb() }
                }
            }

            Label("Wallpaper")
            Segmented(WallpaperMode.entries, settings.wallpaperMode, { it.label }) { settings.wallpaperMode = it }
            when (settings.wallpaperMode) {
                WallpaperMode.PHOTO -> OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text(if (Media.wallpaperFile(context).exists()) "Choose a different photo" else "Choose a photo") }
                WallpaperMode.COLOR -> FlowRow(Modifier.padding(top = 8.dp)) {
                    backgrounds.forEach { (top, bottom, name) ->
                        Swatch(
                            listOf(top, bottom),
                            selected = settings.wallpaperColor == top.toArgb() && settings.wallpaperColor2 == bottom.toArgb(),
                            description = name,
                        ) {
                            settings.wallpaperColor = top.toArgb()
                            settings.wallpaperColor2 = bottom.toArgb()
                            // Keep text readable: light grounds get ink text, dark grounds get cream.
                            val textIsLight = Color(settings.textColor).isLight()
                            if (top.isLight() && textIsLight) settings.textColor = textColors.last().first.toArgb()
                            if (!top.isLight() && !textIsLight) settings.textColor = textColors.first().first.toArgb()
                        }
                    }
                }
                WallpaperMode.SYSTEM -> Text(
                    "Uses the wallpaper set in your phone's settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            LabeledSlider("Dim the wallpaper", "${(settings.dim * 100).toInt()}%", settings.dim, 0f..0.8f) { settings.dim = it }

            // ------------------------------------------------ friction
            SectionTitle("Friction", "Small pauses that break the autopilot.")
            val distractingApps = apps.filter { it.pkg in settings.distracting }.distinctBy { it.pkg }
            if (frictionLocked) {
                Callout(
                    title = "Locked by strict mode",
                    body = "${distractingApps.size} apps pause first  ·  wall waits ${settings.gateSeconds}s  ·  " +
                        "pause lasts ${settings.pauseSeconds}s  ·  " +
                        (if (settings.dailyOpenLimit > 0) "${settings.dailyOpenLimit} opens a day" else "no daily limit"),
                    icon = true,
                ) {
                    if (unlockRemaining > 0) {
                        OutlinedButton(onClick = {}, enabled = false) { Text("Unlocks in ${unlockRemaining}s") }
                    } else {
                        OutlinedButton(onClick = { unlockRemaining = STRICT_UNLOCK_SECONDS }) { Text("Wait 60s to unlock") }
                    }
                }
            } else {
                Label("Apps that pause first")
                Text(
                    if (distractingApps.isEmpty()) "None yet. Shopping, social, video and browsers are good candidates."
                    else distractingApps.joinToString(", ") { it.label },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    FilledTonalButton(onClick = { pickDistracting = true }) { Text("Choose apps") }
                    OutlinedButton(onClick = {
                        val detected = apps.filter { it.kind.isDistracting }.map { it.pkg }.toSet()
                        val added = detected - settings.distracting
                        settings.distracting = settings.distracting + detected
                        Toast.makeText(
                            context,
                            if (added.isEmpty()) "Nothing new found" else "Added ${added.size} apps",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }) { Text("Find them for me") }
                }
                LabeledSlider("Wait before the wall opens", "${settings.gateSeconds}s", settings.gateSeconds.toFloat(), 0f..30f, steps = 29) {
                    settings.gateSeconds = it.toInt()
                }
                LabeledSlider("Pause before distracting apps", "${settings.pauseSeconds}s", settings.pauseSeconds.toFloat(), 5f..60f, steps = 10) {
                    settings.pauseSeconds = it.toInt()
                }
                Label("Daily opens per distracting app")
                Chips(openLimits, settings.dailyOpenLimit, { if (it == 0) "No limit" else "$it" }) { settings.dailyOpenLimit = it }
                ToggleRow(
                    "Search-only wall",
                    supporting = "Hide the full app list so you can't browse it",
                    checked = settings.wallSearchOnly,
                ) { settings.wallSearchOnly = it }
            }

            // ------------------------------------------------ strict
            SectionTitle("Strict mode", "Protects you from yourself in a weak moment.")
            ToggleRow(
                "Strict mode",
                supporting = "Loosening friction needs a 60-second wait, and paused apps can't be unpaused from the wall.",
                checked = settings.strictMode,
                enabled = !frictionLocked,
            ) {
                settings.strictMode = it
                if (it) unlockedThisVisit = true // the lock starts next time Settings opens
            }

            // ------------------------------------------------ screen time
            SectionTitle("Screen time", "Today's total under the clock, and per-app time on the pause screen.")
            ToggleRow("Show screen time", checked = settings.showScreenTime) { settings.showScreenTime = it }
            if (!Usage.hasAccess(context)) {
                OutlinedButton(onClick = { Usage.openAccessSettings(context) }, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Allow usage access")
                }
            } else {
                Text("Usage access is on", style = MaterialTheme.typography.bodyMedium, color = Sage, modifier = Modifier.padding(top = 4.dp))
            }

            // ------------------------------------------------ system
            SectionTitle("Home app")
            if (isDefault) {
                Text("Focus Launcher is your home screen.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                TextButton(onClick = { DefaultLauncher.openHomeSettings(context) }) { Text("Change home app") }
            } else {
                Button(onClick = onMakeDefault) { Text("Make it my home screen") }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Swipe up on the home screen for everything else. Long-press it for these settings.\n" +
                    "Bundled fonts are free to share under the SIL Open Font License 1.1 or Apache 2.0. " +
                    "Cursive and Casual are your phone's own fonts.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
    }

    if (pickEssentials) {
        AppPickerDialog(
            title = "Home screen apps",
            apps = apps,
            initiallySelected = settings.essentials.toSet(),
            keyOf = { it.key },
            maxSelected = MAX_ESSENTIALS,
            onDismiss = { pickEssentials = false },
            onDone = { chosen ->
                // keep the existing order, add new picks at the end
                settings.essentials = settings.essentials.filter { it in chosen } + chosen.filter { it !in settings.essentials }
                pickEssentials = false
            },
        )
    }
    if (arrange) {
        ArrangeDialog(
            labels = settings.essentials.mapNotNull { key -> apps.firstOrNull { it.key == key }?.let { key to it.label } },
            onMove = { key, delta -> settings.essentials = settings.essentials.moved(key, delta) },
            onTop = { key -> settings.essentials = listOf(key) + (settings.essentials - key) },
            onRemove = { key -> settings.essentials = settings.essentials - key },
            onDismiss = { arrange = false },
        )
    }
    if (pickDistracting) {
        AppPickerDialog(
            title = "Apps that pause first",
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

/** A scaled-down home screen that reflects every Look setting live. */
@Composable
private fun HomePreview(settings: LauncherSettings, apps: List<AppEntry>) {
    val look = rememberHomeLook(settings)
    val names = settings.essentials.mapNotNull { key -> apps.firstOrNull { it.key == key }?.label }
        .ifEmpty { listOf("Phone", "Messages", "Camera") }
        .take(3)
    Box(
        Modifier
            .fillMaxWidth()
            .height(264.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black)
            .border(1.dp, Hairline, MaterialTheme.shapes.large)
    ) {
        WallpaperLayer(settings, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            ClockView(settings.clockStyle, settings.use24h, look, settings.clockScale * 0.6f)
            Spacer(Modifier.weight(1f))
            names.forEach {
                Text(
                    it,
                    color = look.color,
                    fontFamily = look.textFamily,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * settings.appTextScale,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }
    }
}

// ------------------------------------------------ building blocks

/** Moves [key] by [delta] places, matching by key so stale entries don't shift the index. */
fun List<String>.moved(key: String, delta: Int): List<String> {
    val index = indexOf(key)
    val target = index + delta
    if (index < 0 || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Spacer(Modifier.height(32.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Cream)
    if (subtitle != null) {
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 4.dp))
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = Cream,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun Callout(title: String, body: String, icon: Boolean = false, action: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Surface1)
            .border(1.dp, Hairline, MaterialTheme.shapes.medium)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Sage, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = Cream)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        action()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) Cream else Muted)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Sage, checkedThumbColor = Ink),
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 18.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Cream, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, color = Muted)
        }
        Slider(value = current, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Surface3, selectedLabelColor = Cream),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(activeContainerColor = Surface3, activeContentColor = Cream),
            ) { Text(label(option), maxLines = 1) }
        }
    }
}

/** Compact reordering: the list scrolls inside the dialog, not the whole settings page. */
@Composable
private fun ArrangeDialog(
    labels: List<Pair<String, String>>,
    onMove: (String, Int) -> Unit,
    onTop: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order on home screen") },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                itemsIndexed(labels, key = { _, item -> item.first }) { index, (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = index > 0, onClickLabel = "Move $label to the top") { onTop(key) }
                                .padding(vertical = 12.dp),
                        )
                        IconButton(onClick = { onMove(key, -1) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move $label up")
                        }
                        IconButton(onClick = { onMove(key, 1) }, enabled = index < labels.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move $label down")
                        }
                        IconButton(onClick = { onRemove(key) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove $label", tint = Muted)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
