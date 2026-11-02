package com.nexus.launcher.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TripOrigin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.data.AppCategory
import com.nexus.launcher.data.AppEntry
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.system.Sfx
import com.nexus.launcher.ui.components.AppGlyph
import com.nexus.launcher.ui.components.HudLabel
import com.nexus.launcher.ui.components.NexusOrb
import com.nexus.launcher.ui.components.OrbMode
import com.nexus.launcher.ui.components.Pill
import com.nexus.launcher.ui.theme.Nx
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ring sizes, innermost first. Most-used apps sit closest to the core. */
private val RING_CAPACITY = listOf(6, 10, 14)
private val RING_RADIUS = listOf(0.30f, 0.56f, 0.82f)
private val RING_SPIN = listOf(1.0f, -0.7f, 0.5f)

@Composable
fun OrbitScreen(graph: NexusGraph, settings: NexusSettings) {
    val context = LocalContext.current
    val apps by graph.apps.apps.collectAsStateWithLifecycle()
    val usage by graph.apps.usage.collectAsStateWithLifecycle()
    val focus by graph.focus.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<AppCategory?>(null) }
    val ranked = remember(apps, usage) { graph.apps.ranked(apps, usage) }

    val open: (AppEntry) -> Unit = { app ->
        if (!graph.focus.allows(app.packageName)) {
            Toast.makeText(context, "${app.label} is held by Focus Mode.", Toast.LENGTH_SHORT).show()
        } else {
            if (settings.sounds) Sfx.open()
            graph.apps.launch(app)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HudLabel("App navigation", Modifier.weight(1f), color = Nx.Cyan)
            val grid = settings.classicGrid || showAll
            IconButton(onClick = {
                if (settings.classicGrid) graph.settings.update { it.copy(classicGrid = false) }
                showAll = !grid
            }) {
                Icon(
                    if (grid) Icons.Outlined.TripOrigin else Icons.Outlined.GridView,
                    contentDescription = if (grid) "Show orbit" else "Show all apps as a grid",
                    tint = Nx.Muted,
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search ${apps.size} apps", color = Nx.Muted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Nx.Cyan) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = Nx.Muted)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Nx.Cyan, unfocusedBorderColor = Nx.Line,
                focusedContainerColor = Nx.Surface, unfocusedContainerColor = Nx.Surface, cursorColor = Nx.Cyan,
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (focus.active && focus.allowedPackages.isNotEmpty()) {
            HudLabel("Focus Mode: only allowed apps are lit", color = Nx.Amber, modifier = Modifier.padding(bottom = 6.dp))
        }

        val searching = query.isNotBlank()
        when {
            searching -> AppGrid(
                ranked.filter { it.label.contains(query.trim(), ignoreCase = true) }
                    .sortedBy { !it.label.startsWith(query.trim(), ignoreCase = true) },
                graph, open,
            )
            settings.classicGrid || showAll -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    item { Pill("All", onClick = { category = null }, selected = category == null) }
                    items(AppCategory.entries.filter { c -> apps.any { it.category == c } }) { c ->
                        Pill(c.label, onClick = { category = c }, selected = category == c)
                    }
                }
                AppGrid(apps.filter { category == null || it.category == category }, graph, open)
            }
            ranked.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Scanning apps…", color = Nx.Muted)
            }
            else -> OrbitalRings(
                apps = ranked.take(RING_CAPACITY.sum()),
                graph = graph,
                reducedMotion = settings.reducedMotion,
                onOpen = open,
                onCore = { showAll = true },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(apps: List<AppEntry>, graph: NexusGraph, onOpen: (AppEntry) -> Unit) {
    if (apps.isEmpty()) {
        Text("No apps match.", color = Nx.Muted, modifier = Modifier.padding(16.dp))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(apps, key = { it.component.flattenToString() }) { app ->
            Column(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .combinedClickable(
                        role = Role.Button,
                        onClick = { onOpen(app) },
                        onLongClick = { graph.apps.openAppInfo(app) },
                        onLongClickLabel = "App info",
                    )
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppGlyph(app.icon, app.label, dimmed = !graph.focus.allows(app.packageName))
                Spacer(Modifier.height(6.dp))
                Text(
                    app.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Apps on three concentric orbits. Drag in a circle to spin them (with momentum); the
 * rings turn at different speeds and directions. Tap the core for the full grid.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrbitalRings(
    apps: List<AppEntry>,
    graph: NexusGraph,
    reducedMotion: Boolean,
    onOpen: (AppEntry) -> Unit,
    onCore: () -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Split apps across rings, innermost first.
    val rings = remember(apps) {
        var start = 0
        RING_CAPACITY.map { cap ->
            val slice = apps.drop(start).take(cap)
            start += cap
            slice
        }.filter { it.isNotEmpty() }
    }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val radiusPx = with(density) { side.toPx() } / 2f
        val iconSize = if (side < 340.dp) 44.dp else 52.dp

        Box(
            Modifier
                .size(side)
                .pointerInput(Unit) {
                    val tracker = VelocityTracker()
                    var lastAngle = 0f
                    var accumulated = 0f
                    detectDragGestures(
                        onDragStart = { o ->
                            scope.launch { rotation.stop() }
                            tracker.resetTracking()
                            lastAngle = angleOf(o, size.width / 2f, size.height / 2f)
                        },
                        onDragEnd = {
                            // The tracker records rotation in degrees, so its x velocity is degrees per second.
                            val speed = tracker.calculateVelocity().x
                            if (!reducedMotion) scope.launch { rotation.animateDecay(speed.coerceIn(-900f, 900f), exponentialDecay(1.6f)) }
                        },
                    ) { change, _ ->
                        val a = angleOf(change.position, size.width / 2f, size.height / 2f)
                        var d = a - lastAngle
                        if (d > 180f) d -= 360f
                        if (d < -180f) d += 360f
                        lastAngle = a
                        accumulated += d
                        tracker.addPosition(change.uptimeMillis, Offset(accumulated, 0f))
                        scope.launch { rotation.snapTo(rotation.value + d) }
                        change.consume()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                rings.indices.forEach { i ->
                    val r = radiusPx * RING_RADIUS[i]
                    drawCircle(Nx.Line, radius = r, center = center, style = Stroke(1.dp.toPx()))
                    // Tick marks give the rings a machined, instrument-like feel.
                    for (k in 0 until 72) {
                        val a = (k * 5f + rotation.value * RING_SPIN[i]) * PI.toFloat() / 180f
                        val len = if (k % 6 == 0) 6.dp.toPx() else 2.dp.toPx()
                        drawLine(
                            Nx.CyanDim,
                            Offset(center.x + (r - len) * cos(a), center.y + (r - len) * sin(a)),
                            Offset(center.x + r * cos(a), center.y + r * sin(a)),
                            strokeWidth = 1f,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .size(side * 0.36f)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onCore() }
                    .semantics { contentDescription = "Show all apps" },
                contentAlignment = Alignment.Center,
            ) {
                NexusOrb(OrbMode.IDLE, size = side * 0.36f, reducedMotion = reducedMotion)
                HudLabel("All", color = Nx.Obsidian)
            }

            rings.forEachIndexed { i, ring ->
                val r = radiusPx * RING_RADIUS[i]
                ring.forEachIndexed { j, app ->
                    val baseDeg = j * 360f / ring.size + i * 17f - 90f
                    val dimmed = !graph.focus.allows(app.packageName)
                    Box(
                        Modifier
                            .offset {
                                val a = (baseDeg + rotation.value * RING_SPIN[i]) * PI.toFloat() / 180f
                                IntOffset((r * cos(a)).roundToInt(), (r * sin(a)).roundToInt())
                            }
                            .size(iconSize)
                            .clip(CircleShape)
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { onOpen(app) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    graph.apps.openAppInfo(app)
                                },
                                onLongClickLabel = "App info",
                            ),
                    ) {
                        AppGlyph(app.icon, app.label, size = iconSize, dimmed = dimmed)
                    }
                }
            }
        }
        HudLabel(
            "Spin the rings · tap the core for every app",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
        )
    }
}

private fun angleOf(p: Offset, cx: Float, cy: Float): Float =
    (atan2(p.y - cy, p.x - cx) * 180f / PI.toFloat())
