package com.nexus.launcher.ui

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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

    val runAction: (NexusAction) -> Unit = { action ->
        fun start(intent: Intent) = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(context, "No app on this phone can do that.", Toast.LENGTH_SHORT).show() }
        when (action) {
            is NexusAction.OpenApp -> {
                val app = graph.apps.find(action.name)
                when {
                    app == null -> Toast.makeText(context, "Couldn't find ${action.name}.", Toast.LENGTH_SHORT).show()
                    !graph.focus.allows(app.packageName) -> Toast.makeText(context, "${app.label} is held by Focus Mode.", Toast.LENGTH_SHORT).show()
                    else -> graph.apps.launch(app)
                }
            }
            is NexusAction.StartFocus -> { graph.focus.start(action.minutes); screen = Screen.FOCUS }
            NexusAction.StopFocus -> graph.focus.stop()
            is NexusAction.Navigate -> screen = when (action.screen) {
                "apps", "orbit" -> Screen.ORBIT
                "alerts", "notifications" -> Screen.ALERTS
                "focus" -> Screen.FOCUS
                "command" -> Screen.COMMAND
                "settings" -> Screen.SETTINGS
                else -> Screen.HOME
            }
            is NexusAction.SetAlarm -> start(
                Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
            )
            is NexusAction.SetTimer -> start(
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
            )
            is NexusAction.Dial -> start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.number}")))
            is NexusAction.WebSearch -> start(
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, action.query),
            )
        }
        Unit
    }

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
            if (screen != Screen.SETTINGS) OrbitalDock(screen, settings.reducedMotion, onSelect = ::go)
        }
    }
}

/** Floating dock: four destinations around a central NEXUS core that opens the Command Centre. */
@Composable
private fun OrbitalDock(current: Screen, reducedMotion: Boolean, onSelect: (Screen) -> Unit) {
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
            DockItem(Icons.Outlined.NotificationsNone, Screen.ALERTS, current, onSelect)
            DockItem(Icons.Outlined.CenterFocusStrong, Screen.FOCUS, current, onSelect)
        }
        Box(
            Modifier
                .padding(bottom = 6.dp)
                .size(76.dp)
                .clip(CircleShape)
                .background(Nx.Obsidian)
                .border(1.dp, if (current == Screen.COMMAND) Nx.Cyan else Nx.Line, CircleShape)
                .clickable(role = Role.Button) { onSelect(Screen.COMMAND) }
                .semantics { contentDescription = "Open NEXUS AI Command Centre" },
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
private fun DockItem(icon: ImageVector, target: Screen, current: Screen, onSelect: (Screen) -> Unit) {
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
        Icon(icon, contentDescription = target.label, tint = if (active) Nx.Cyan else Nx.Muted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            target.label.uppercase(), fontFamily = Hud, fontSize = 9.sp, letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium, color = if (active) Nx.Cyan else Nx.Muted,
        )
    }
}
