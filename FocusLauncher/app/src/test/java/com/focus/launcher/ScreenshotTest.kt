package com.focus.launcher

import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.graphics.Color
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

/**
 * Renders every screen with fake apps so the design can be reviewed without a phone.
 * PNGs land in app/build/screens/ (CI copies them to FocusLauncher/screenshots/).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = LocalDateTime.of(2026, 11, 4, 9, 41, 12)

    private val apps: List<AppEntry> by lazy {
        val me = Process.myUserHandle()
        fun app(label: String, pkg: String, kind: AppKind = AppKind.OTHER, clone: Boolean = false) =
            AppEntry(
                key = "$pkg/.Main" + if (clone) "@10" else "",
                pkg = pkg, cls = ".Main", label = label, kind = kind, user = me, isClone = clone,
            )
        listOf(
            app("Phone", "com.android.dialer"),
            app("Messages", "com.google.android.apps.messaging"),
            app("WhatsApp", "com.whatsapp"),
            app("WhatsApp (2)", "com.whatsapp", clone = true),
            app("Camera", "com.android.camera"),
            app("Maps", "com.google.android.apps.maps"),
            app("Calendar", "com.google.android.calendar"),
            app("Gmail", "com.google.android.gm"),
            app("Bank", "com.bank.app"),
            app("Notes", "com.notes"),
            app("Spotify", "com.spotify.music"),
            app("Instagram", "com.instagram.android", AppKind.SOCIAL),
            app("Amazon Shopping", "in.amazon.mShop.android.shopping", AppKind.SHOPPING),
            app("Flipkart", "com.flipkart.android", AppKind.SHOPPING),
            app("Chrome", "com.android.chrome", AppKind.BROWSER),
            app("YouTube", "com.google.android.youtube", AppKind.SOCIAL),
            app("Settings", "com.android.settings"),
        ).sortedBy { it.label.lowercase() }
    }

    private fun settings(block: LauncherSettings.() -> Unit = {}): LauncherSettings =
        LauncherSettings(compose.activity).apply {
            setupDone = true
            onboardingSeen = true
            essentials = listOf("Phone", "Messages", "WhatsApp", "WhatsApp (2)", "Camera", "Maps").map { label ->
                apps.first { it.label == label }.key
            }
            distracting = apps.filter { it.kind.isDistracting }.map { it.pkg }.toSet()
            wallpaperMode = WallpaperMode.COLOR
            wallpaperColor = 0xFF0E1A1F.toInt()
            wallpaperColor2 = 0xFF2B4C59.toInt()
            dim = 0.1f
            showScreenTime = true
            block()
        }

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalFixedNow provides now) {
                FocusTheme { content() }
            }
        }
        compose.mainClock.advanceTimeBy(1_200)
        compose.onRoot().captureRoboImage("build/screens/$name.png")
    }

    @Composable
    private fun HomeScreenHost(s: LauncherSettings, isDefault: Boolean) {
        HomeScreen(
            settings = s, apps = apps, screenTimeMs = 2 * 3_600_000L + 14 * 60_000L,
            isDefault = isDefault, onMakeDefault = {}, onOpen = {}, onOpenWall = {}, onOpenSettings = {},
        )
    }

    @Test fun home_default() {
        val s = settings { homeNote = "Read, don't scroll." }
        s.recordResisted(); s.recordResisted()
        shot("01_home_default") { HomeScreenHost(s, true) }
    }

    @Test fun home_words_serif() {
        val s = settings {
            clockStyle = ClockStyle.WORDS; clockFont = FontChoice.INSTRUMENT_SERIF; textFont = FontChoice.INSTRUMENT_SERIF
            wallpaperColor = 0xFF141F16.toInt(); wallpaperColor2 = 0xFF3F5E44.toInt()
        }
        shot("02_home_words_serif") { HomeScreenHost(s, true) }
    }

    @Test fun home_analog_paper() {
        val s = settings {
            clockStyle = ClockStyle.ANALOG
            wallpaperColor = 0xFFF3EEE5.toInt(); wallpaperColor2 = 0xFFE2D8C7.toInt(); dim = 0f
            textColor = Color(0xFF151514).toArgb(); textFont = FontChoice.FRAUNCES
        }
        shot("03_home_analog_paper") { HomeScreenHost(s, true) }
    }

    @Test fun home_stacked_two_columns() {
        val s = settings {
            clockStyle = ClockStyle.STACKED; clockFont = FontChoice.BEBAS_NEUE; homeColumns = 2
            essentials = apps.filter { !it.kind.isDistracting }.map { it.key }
            wallpaperColor = 0xFF1E1529.toInt(); wallpaperColor2 = 0xFF5E4570.toInt()
        }
        shot("04_home_stacked_two_columns") { HomeScreenHost(s, true) }
    }

    @Test fun home_first_run_not_default() {
        val s = settings { onboardingSeen = false; clockFont = FontChoice.SORA; clockWeight = 300 }
        shot("05_home_not_default") { HomeScreenHost(s, false) }
    }

    @Test fun gate() {
        shot("06_gate") { GateScreen(seconds = 5, onContinue = {}, onBack = {}) }
    }

    @Test fun wall_empty() {
        val s = settings()
        shot("07_wall_empty") { WallScreen(s, apps, onOpen = {}, onBack = {}) }
    }

    @Test fun wall_search() {
        val s = settings { dailyOpenLimit = 1 }
        s.recordOpen("com.flipkart.android")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalFixedNow provides now) {
                FocusTheme { WallScreen(s, apps, onOpen = {}, onBack = {}) }
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("a")
        compose.mainClock.advanceTimeBy(1_200)
        compose.onRoot().captureRoboImage("build/screens/08_wall_search.png")
    }

    @Test fun pause() {
        val s = settings { pauseSeconds = 15; dailyOpenLimit = 5 }
        s.recordOpen("com.instagram.android"); s.recordOpen("com.instagram.android")
        val app = apps.first { it.label == "Instagram" }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalFixedNow provides now) {
                FocusTheme { PauseScreen(app, s, onProceed = {}, onCancel = {}) }
            }
        }
        compose.onNodeWithText("Bored").performScrollTo()
        compose.onNodeWithText("Something specific").also { it.performScrollTo() }
        compose.mainClock.advanceTimeBy(1_200)
        compose.onRoot().captureRoboImage("build/screens/09_pause.png")
    }

    @Test fun pause_limit_reached() {
        val s = settings { dailyOpenLimit = 3 }
        repeat(3) { s.recordOpen("in.amazon.mShop.android.shopping") }
        val app = apps.first { it.label == "Amazon Shopping" }
        shot("10_pause_limit") { PauseScreen(app, s, onProceed = {}, onCancel = {}) }
    }

    @Test fun settings_top() {
        val s = settings()
        shot("11_settings_top") { SettingsScreen(s, apps, isDefault = false, onMakeDefault = {}, onBack = {}) }
    }

    @Test fun settings_look() {
        val s = settings { clockFont = FontChoice.FRAUNCES; clockWeight = 300 }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalFixedNow provides now) {
                FocusTheme { SettingsScreen(s, apps, isDefault = true, onMakeDefault = {}, onBack = {}) }
            }
        }
        compose.onNodeWithText("Clock style").performScrollTo()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onRoot().captureRoboImage("build/screens/12_settings_look.png")
    }

    @Test fun settings_fonts() {
        val s = settings { clockFont = FontChoice.FRAUNCES; clockWeight = 300 }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalFixedNow provides now) {
                FocusTheme { SettingsScreen(s, apps, isDefault = true, onMakeDefault = {}, onBack = {}) }
            }
        }
        compose.onNodeWithText("Text colour").performScrollTo()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onRoot().captureRoboImage("build/screens/13_settings_fonts.png")
    }

    @Test fun settings_strict_locked() {
        val s = settings { strictMode = true; dailyOpenLimit = 5 }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalFixedNow provides now) {
                FocusTheme { SettingsScreen(s, apps, isDefault = true, onMakeDefault = {}, onBack = {}) }
            }
        }
        compose.onNodeWithText("Locked by strict mode").performScrollTo()
        compose.mainClock.advanceTimeBy(1_200)
        compose.onRoot().captureRoboImage("build/screens/14_settings_strict.png")
    }
}
