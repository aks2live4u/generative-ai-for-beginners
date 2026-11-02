package com.nexus.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextOverflow
import com.nexus.launcher.system.NexusNotificationService
import com.nexus.launcher.system.Tier
import com.nexus.launcher.ui.screens.Countdown
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusGraph
import com.nexus.launcher.ai.NexusAction
import com.nexus.launcher.system.Sfx
import com.nexus.launcher.ui.components.NexusBackground
import com.nexus.launcher.ui.components.NexusOrb
import com.nexus.launcher.ui.components.OrbMode
import com.nexus.launcher.ui.screens.AlertsScreen
import com.nexus.launcher.ui.screens.CommandCentreScreen
import com.nexus.launcher.ui.screens.CommandSession
import com.nexus.launcher.ui.screens.FocusScreen
import com.nexus.launcher.ui.screens.HomeScreen
import com.nexus.launcher.ui.screens.OrbitScreen
import com.nexus.launcher.ui.screens.SettingsScreen
import com.nexus.launcher.ui.theme.Hud
import com.nexus.launcher.ui.theme.Nx

enum class Screen(val label: String) { HOME("Home"), ORBIT("Apps"), COMMAND("NEXUS"), ALERTS("Alerts"), FOCUS("Focus"), SETTINGS("Settings") }

@Composable
fun NexusApp(graph: NexusGraph) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val settings by graph.settings.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val session = remember { CommandSession() }

    fun go(to: Screen) {
        if (to == screen) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (settings.sounds) Sfx.tap()
        screen = to
    }

    LaunchedEffect(Unit) { graph.homePresses.collect { screen = Screen.HOME } }
    // Back always returns Home; the launcher itself never closes.
    BackHandler { if (screen != Screen.HOME) screen = Screen.HOME }

    val runAction = rememberActionRunner(graph, session, onScreen = { screen = it })

    Box(Modifier.fillMaxSize()) {
        NexusBackground(reducedMotion = settings.reducedMotion)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (settings.reducedMotion) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    else (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith
                        (fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 1.03f))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "screen",
            ) { target ->
                when (target) {
                    Screen.HOME -> HomeScreen(graph, settings, onNavigate = ::go, onAsk = { prompt ->
                        session.pendingPrompt = prompt
                        go(Screen.COMMAND)
                    })
                    Screen.ORBIT -> OrbitScreen(graph, settings)
                    Screen.COMMAND -> CommandCentreScreen(graph, settings, session, runAction)
                    Screen.ALERTS -> AlertsScreen(graph, settings)
                    Screen.FOCUS -> FocusScreen(graph, settings)
                    Screen.SETTINGS -> SettingsScreen(graph, settings, onDone = { go(Screen.HOME) })
                }
            }
            session.countdown?.let { CountdownBanner(it, session) }
            if (screen != Screen.SETTINGS) {
                val notifications by NexusNotificationService.items.collectAsStateWithLifecycle()
                OrbitalDock(
                    current = screen,
                    reducedMotion = settings.reducedMotion,
                    alertCount = notifications.count { it.tier == Tier.PRIORITY },
                    onSelect = ::go,
                    onOrb = {
                        // One tap: open the Command Centre and start listening (or stop, if already listening).
                        if (settings.sounds) Sfx.listen()
                        session.listenRequested = true
                        screen = Screen.COMMAND
                    },
                )
            }
        }
    }
}

/** Floating dock: four destinations around a central NEXUS core that opens the Command Centre. */
@Composable
private fun OrbitalDock(
    current: Screen,
    reducedMotion: Boolean,
    alertCount: Int,
    onSelect: (Screen) -> Unit,
    onOrb: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp).navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(Brush.verticalGradient(listOf(Nx.SurfaceHi.copy(alpha = 0.95f), Nx.Surface.copy(alpha = 0.95f))))
                .border(1.dp, Nx.Line, RoundedCornerShape(34.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DockItem(Icons.Outlined.Home, Screen.HOME, current, onSelect)
            DockItem(Icons.Outlined.Apps, Screen.ORBIT, current, onSelect)
            Spacer(Modifier.size(76.dp))
            DockItem(Icons.Outlined.NotificationsNone, Screen.ALERTS, current, onSelect, badge = alertCount)
            DockItem(Icons.Outlined.CenterFocusStrong, Screen.FOCUS, current, onSelect)
        }
        Box(
            Modifier
                .padding(bottom = 6.dp)
                .size(76.dp)
                .clip(CircleShape)
                .background(Nx.Obsidian)
                .border(1.dp, if (current == Screen.COMMAND) Nx.Cyan else Nx.Line, CircleShape)
                .clickable(role = Role.Button, onClick = onOrb)
                .semantics { contentDescription = "Talk to NEXUS" },
            contentAlignment = Alignment.Center,
        ) {
            NexusOrb(
                mode = if (current == Screen.COMMAND) OrbMode.LISTENING else OrbMode.IDLE,
                size = 70.dp,
                reducedMotion = reducedMotion,
            )
        }
    }
}

@Composable
private fun DockItem(icon: ImageVector, target: Screen, current: Screen, onSelect: (Screen) -> Unit, badge: Int = 0) {
    val active = current == target
    Column(
        Modifier
            .size(width = 60.dp, height = 60.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Tab) { onSelect(target) }
            .semantics { selected = active },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(
                icon,
                contentDescription = if (badge > 0) "${target.label}, $badge need you" else target.label,
                tint = if (active) Nx.Cyan else Nx.Muted,
                modifier = Modifier.size(24.dp),
            )
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-6).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Nx.Amber),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (badge > 9) "9+" else "$badge", color = Nx.Obsidian, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            target.label.uppercase(), fontFamily = Hud, fontSize = 9.sp, letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium, color = if (active) Nx.Cyan else Nx.Muted,
        )
    }
}

/** The 3-second window before NEXUS places a call or sends a text. Visible on every screen. */
@Composable
private fun CountdownBanner(c: Countdown, session: CommandSession) {
    var remaining by remember(c) { mutableStateOf(c.seconds) }
    LaunchedEffect(c) {
        while (true) {
            remaining = (((c.endsAt - System.currentTimeMillis()) + 999) / 1000).toInt().coerceAtLeast(0)
            if (remaining == 0) break
            delay(100)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Nx.SurfaceHi)
            .border(1.dp, Nx.Cyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$remaining", fontFamily = Hud, fontSize = 28.sp, color = Nx.Cyan, fontWeight = FontWeight.Light)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("${c.verb} ${c.who}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Nx.Text, maxLines = 1)
            Text(c.detail, fontSize = 13.sp, color = Nx.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = { session.runNow() }) { Text("Now", color = Nx.Cyan) }
        Box(
            Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Nx.Amber)
                .clickable(role = Role.Button) { session.cancelCountdown() }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Cancel", color = Nx.Obsidian, fontWeight = FontWeight.SemiBold)
        }
    }
}
