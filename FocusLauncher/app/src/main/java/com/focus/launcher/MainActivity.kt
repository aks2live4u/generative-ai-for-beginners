package com.focus.launcher

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data object Gate : Screen
    data object Wall : Screen
    data class Pause(val app: AppEntry) : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {

    companion object {
        /** Read by the session reminder: no nudge needed if you're already home. */
        @Volatile
        var inForeground = false
    }

    private lateinit var settings: LauncherSettings
    private val apps = mutableStateOf<List<AppEntry>>(emptyList())
    private val screen = mutableStateOf<Screen>(Screen.Home)
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        settings = LauncherSettings(this)
        setContent {
            FocusTheme {
                LauncherRoot(
                    settings = settings,
                    apps = apps.value,
                    screen = screen.value,
                    resumeTick = resumeTick.intValue,
                    navigate = { screen.value = it },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        resumeTick.intValue++
        refreshApps()
    }

    override fun onPause() {
        super.onPause()
        inForeground = false
    }

    override fun onStop() {
        super.onStop()
        // Never leave the hidden wall open behind your back.
        if (screen.value != Screen.Settings) screen.value = Screen.Home
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        screen.value = Screen.Home // Home button pressed while we're already in front.
    }

    private fun refreshApps() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { AppRepository.load(this@MainActivity) }
            apps.value = list
            if (!settings.setupDone && list.isNotEmpty()) {
                settings.essentials = AppRepository.defaultEssentials(this@MainActivity, list)
                settings.distracting = list.filter { it.kind.isDistracting }.map { it.pkg }.toSet()
                settings.setupDone = true
            }
        }
    }
}

@Composable
fun LauncherRoot(
    settings: LauncherSettings,
    apps: List<AppEntry>,
    screen: Screen,
    resumeTick: Int,
    navigate: (Screen) -> Unit,
) {
    val context = LocalContext.current

    fun launchNow(app: AppEntry, sessionMinutes: Int = 0) {
        if (AppRepository.launch(context, app)) {
            settings.recordOpen(app.pkg)
            if (app.pkg in settings.distracting) SessionReminder.schedule(context, app, sessionMinutes)
        } else {
            Toast.makeText(context, "${app.label} couldn't be opened. It may have been removed.", Toast.LENGTH_SHORT).show()
        }
        navigate(Screen.Home)
    }

    fun open(app: AppEntry) {
        if (app.pkg in settings.distracting) navigate(Screen.Pause(app)) else launchNow(app)
    }

    fun openWall() {
        navigate(if (settings.gateSeconds > 0) Screen.Gate else Screen.Wall)
    }

    fun skip() {
        settings.recordResisted()
        navigate(Screen.Home)
    }

    // A launcher never "exits" on Back. Backing out of a pause counts as a skipped distraction.
    BackHandler(enabled = true) {
        when (screen) {
            Screen.Home -> Unit
            Screen.Gate, is Screen.Pause -> skip()
            else -> navigate(Screen.Home)
        }
    }

    // Checked synchronously so the "make default" card never flashes on a launcher that is default.
    var isDefault by remember { mutableStateOf(DefaultLauncher.isDefault(context)) }
    LaunchedEffect(resumeTick) { isDefault = DefaultLauncher.isDefault(context) }
    val makeDefault = rememberMakeDefaultRequest { isDefault = DefaultLauncher.isDefault(context) }

    // System bar icons follow what's behind them: dark icons on light wallpapers.
    val view = LocalView.current
    val darkIcons = screen == Screen.Home && !Color(settings.textColor).isLight()
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }

    var screenTime by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(resumeTick, settings.showScreenTime) {
        screenTime = if (settings.showScreenTime && Usage.hasAccess(context)) {
            withContext(Dispatchers.IO) { Usage.screenTimeToday(context) }
        } else null
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            settings = settings,
            apps = apps,
            screenTimeMs = screenTime,
            isDefault = isDefault,
            onMakeDefault = makeDefault,
            onOpen = ::open,
            onOpenWall = ::openWall,
            onOpenSettings = { navigate(Screen.Settings) },
        )
        Screen.Gate -> GateScreen(
            seconds = settings.gateSeconds,
            onContinue = { navigate(Screen.Wall) },
            onBack = ::skip,
        )
        Screen.Wall -> WallScreen(
            settings = settings,
            apps = apps,
            onOpen = ::open,
            onBack = { navigate(Screen.Home) },
        )
        is Screen.Pause -> PauseScreen(
            app = screen.app,
            settings = settings,
            onProceed = { minutes -> launchNow(screen.app, minutes) },
            onCancel = ::skip,
        )
        Screen.Settings -> SettingsScreen(
            settings = settings,
            apps = apps,
            isDefault = isDefault,
            onMakeDefault = makeDefault,
            onBack = { navigate(Screen.Home) },
        )
    }
}
