package com.focus.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import java.time.LocalDate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class ClockStyle(val label: String) {
    DIGITAL("Digital"), STACKED("Stacked"), WORDS("Words"), ANALOG("Analog"), MINIMAL("Minimal")
}

enum class WallpaperMode(val label: String) { SYSTEM("System"), PHOTO("Photo"), COLOR("Color") }

enum class FontChoice(val label: String) {
    DEFAULT("Sans"), LIGHT("Light"), THIN("Thin"), CONDENSED("Condensed"), SERIF("Serif"),
    MONO("Mono"), CASUAL("Casual"), CURSIVE("Cursive"), CUSTOM("Imported")
}

/**
 * Every setting is backed by SharedPreferences *and* a Compose snapshot state,
 * so changing something in Settings instantly updates the home screen.
 */
class LauncherSettings(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("focus_launcher", Context.MODE_PRIVATE)

    private class PrefState<T>(initial: T, private val save: (T) -> Unit) : ReadWriteProperty<Any?, T> {
        private val state = mutableStateOf(initial)
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            state.value = value
            save(value)
        }
    }

    private fun string(key: String, def: String) =
        PrefState(sp.getString(key, def) ?: def) { sp.edit().putString(key, it).apply() }

    private fun bool(key: String, def: Boolean) =
        PrefState(sp.getBoolean(key, def)) { sp.edit().putBoolean(key, it).apply() }

    private fun int(key: String, def: Int) =
        PrefState(sp.getInt(key, def)) { sp.edit().putInt(key, it).apply() }

    private fun float(key: String, def: Float) =
        PrefState(sp.getFloat(key, def)) { sp.edit().putFloat(key, it).apply() }

    // ---- setup & app lists ----
    var setupDone by bool("setup_done", false)
    private var essentialsRaw by string("essentials", "")
    private var distractingRaw by string("distracting", "")

    /** Ordered component keys ("package/activity") shown on the home screen. */
    var essentials: List<String>
        get() = essentialsRaw.split("|").filter { it.isNotEmpty() }
        set(value) { essentialsRaw = value.joinToString("|") }

    /** Package names that get the mindful pause before opening. */
    var distracting: Set<String>
        get() = distractingRaw.split("|").filter { it.isNotEmpty() }.toSet()
        set(value) { distractingRaw = value.joinToString("|") }

    // ---- friction ----
    var gateSeconds by int("gate_seconds", 5)
    var pauseSeconds by int("pause_seconds", 15)
    var wallSearchOnly by bool("wall_search_only", true)

    // ---- look ----
    private var clockStyleName by string("clock_style", ClockStyle.DIGITAL.name)
    var clockStyle: ClockStyle
        get() = runCatching { ClockStyle.valueOf(clockStyleName) }.getOrDefault(ClockStyle.DIGITAL)
        set(value) { clockStyleName = value.name }

    private var fontChoiceName by string("font_choice", FontChoice.LIGHT.name)
    var fontChoice: FontChoice
        get() = runCatching { FontChoice.valueOf(fontChoiceName) }.getOrDefault(FontChoice.LIGHT)
        set(value) { fontChoiceName = value.name }

    private var wallpaperModeName by string("wallpaper_mode", WallpaperMode.COLOR.name)
    var wallpaperMode: WallpaperMode
        get() = runCatching { WallpaperMode.valueOf(wallpaperModeName) }.getOrDefault(WallpaperMode.COLOR)
        set(value) { wallpaperModeName = value.name }

    var use24h by bool("use_24h", false)
    var showDate by bool("show_date", true)
    var showScreenTime by bool("show_screen_time", true)
    var textColor by int("text_color", 0xFFE8E2D6.toInt())
    var clockScale by float("clock_scale", 1f)
    var wallpaperColor by int("wallpaper_color", 0xFF000000.toInt())
    var wallpaperColor2 by int("wallpaper_color2", 0xFF000000.toInt())
    var dim by float("dim", 0.25f)
    var homeNote by string("home_note", "")
    var homeColumns by int("home_columns", 1)
    var appTextScale by float("app_text_scale", 1f)

    /** Bumped whenever the wallpaper / font file changes so the UI reloads it. */
    var wallpaperVersion by int("wallpaper_version", 0)
    var fontVersion by int("font_version", 0)

    // ---- daily counters ----
    private var countersDay by string("counters_day", "")
    private var opensRaw by string("opens", "")
    private var resistedRaw by int("resisted", 0)

    private fun today() = LocalDate.now().toString()

    private fun rollDay() {
        if (countersDay != today()) {
            countersDay = today()
            opensRaw = ""
            resistedRaw = 0
        }
    }

    private fun opensMap(): Map<String, Int> =
        opensRaw.split("|").mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap()

    fun opensToday(pkg: String): Int = if (countersDay != today()) 0 else opensMap()[pkg] ?: 0

    fun recordOpen(pkg: String) {
        rollDay()
        val map = opensMap().toMutableMap()
        map[pkg] = (map[pkg] ?: 0) + 1
        opensRaw = map.entries.joinToString("|") { "${it.key}=${it.value}" }
    }

    val resistedToday: Int get() = if (countersDay != today()) 0 else resistedRaw

    fun recordResisted() {
        rollDay()
        resistedRaw += 1
    }
}
