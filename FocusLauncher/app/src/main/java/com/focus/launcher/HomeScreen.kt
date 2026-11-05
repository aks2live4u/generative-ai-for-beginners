package com.focus.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/** Lets previews and screenshot tests pin the clock to a fixed moment. */
val LocalFixedNow = staticCompositionLocalOf<LocalDateTime?> { null }

/** Everything the home screen needs to draw its type, resolved once per settings change. */
class HomeLook(
    val clockFamily: FontFamily,
    val clockWeight: FontWeight,
    val textFamily: FontFamily,
    val color: Color,
    /** Optical size correction for the clock font. */
    val clockSize: Float,
    /** Text font's optical correction times the user's "App name size". */
    val textSize: Float,
    /** Text font's optical correction alone, for small chrome like the footer. */
    val textFontSize: Float,
)

@Composable
fun rememberHomeLook(settings: LauncherSettings): HomeLook {
    val context = LocalContext.current
    val clockFont = settings.clockFont
    val clockWeight = settings.clockWeight
    val textFont = settings.textFont
    val version = settings.fontVersion
    val color = settings.textColor
    val scale = settings.appTextScale
    return remember(clockFont, clockWeight, textFont, version, color, scale) {
        HomeLook(
            clockFamily = Fonts.family(context, clockFont, clockWeight, version),
            clockWeight = Fonts.weight(clockFont, clockWeight),
            textFamily = Fonts.family(context, textFont, 400, version),
            color = Color(color),
            clockSize = clockFont.size,
            textSize = textFont.size * scale,
            textFontSize = textFont.size,
        )
    }
}

@Composable
fun HomeScreen(
    settings: LauncherSettings,
    apps: List<AppEntry>,
    screenTimeMs: Long?,
    isDefault: Boolean,
    onMakeDefault: () -> Unit,
    onOpen: (AppEntry) -> Unit,
    onOpenWall: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val look = rememberHomeLook(settings)
    val essentials = remember(settings.essentials, apps) {
        settings.essentials.mapNotNull { key -> apps.firstOrNull { it.key == key } }
    }
    val openWall by rememberUpdatedState(onOpenWall)
    val openSettings by rememberUpdatedState(onOpenSettings)
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { openSettings() }) }
            .pointerInput(swipeThreshold) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -swipeThreshold) openWall() },
                    onVerticalDrag = { _, dy -> dragged += dy },
                )
            }
    ) {
        WallpaperLayer(settings)

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            when {
                !isDefault -> HomeNotice(
                    look = look,
                    title = "Make this your home screen",
                    body = "So the Home button lands here, not on your old icons.",
                    action = "Set",
                    onAction = onMakeDefault,
                )
                !settings.onboardingSeen -> HomeNotice(
                    look = look,
                    title = "Only what matters lives here",
                    body = "Swipe up for everything else. Long-press anywhere for settings.",
                    action = "Got it",
                    onAction = { settings.onboardingSeen = true },
                )
            }

            Spacer(Modifier.height(12.dp))
            ClockView(settings.clockStyle, settings.use24h, look, settings.clockScale)
            HomeSubtitle(settings, look, screenTimeMs)
            WorldClocksRow(settings, look)

            // Essentials sit low, in thumb reach, and scroll when there are many.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (essentials.isEmpty()) {
                    if (apps.isNotEmpty()) EmptyHome(look, onOpenSettings)
                } else {
                    EssentialsList(essentials, settings, look, onOpen)
                }
            }

            HomeFooter(look, onOpenWall, onOpenSettings)
        }
    }
}

@Composable
private fun HomeSubtitle(settings: LauncherSettings, look: HomeLook, screenTimeMs: Long?) {
    // Zone 1 continues: the date hugs the clock.
    if (settings.showDate) {
        val now = rememberNow(minuteOnly = true)
        Text(
            now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            color = look.color.copy(alpha = 0.8f),
            fontFamily = look.textFamily,
            fontSize = (18 * look.textSize).sp,
            letterSpacing = 0.2.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    // Zone 2: today. The note reads as a quote, set off by space and a short rule.
    if (settings.homeNote.isNotBlank()) {
        Box(
            Modifier
                .padding(top = 20.dp)
                .width(28.dp)
                .height(1.5.dp)
                .background(look.color.copy(alpha = 0.45f))
        )
        Text(
            settings.homeNote,
            color = look.color,
            fontFamily = look.textFamily,
            fontSize = (21 * look.textSize).sp,
            lineHeight = (28 * look.textSize).sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    val stats = buildList {
        if (screenTimeMs != null) add("${Usage.format(screenTimeMs)} today")
        val skipped = settings.resistedToday
        if (skipped > 0) add("$skipped skipped")
    }
    if (stats.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = if (settings.homeNote.isNotBlank()) 12.dp else 18.dp),
        ) {
            stats.forEach { StatPill(it, look) }
        }
    }
}

/** Small factual detail in the clean UI face, on a soft tonal pill: clearly not an app name. */
@Composable
fun StatPill(text: String, look: HomeLook) {
    val ink = if (look.color.isLight()) Color.White else Color.Black
    Text(
        text,
        color = look.color.copy(alpha = 0.85f),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(CircleShape)
            .background(ink.copy(alpha = 0.09f))
            .border(1.dp, ink.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun EssentialsList(
    essentials: List<AppEntry>,
    settings: LauncherSettings,
    look: HomeLook,
    onOpen: (AppEntry) -> Unit,
) {
    val columns = settings.homeColumns.coerceIn(1, 2)
    val baseSize = when {
        columns == 2 -> 22f
        essentials.size <= 7 -> 28f
        essentials.size <= 12 -> 25f
        else -> 22f
    }
    // Includes the font's optical correction, so Caveat at 100% looks as big as a sans at 100%.
    val size = (baseSize * look.textSize).sp
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        // A fine rule right above the first app marks where "today" ends and your apps begin.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .height(1.dp)
                .background(look.color.copy(alpha = 0.16f))
        )
        essentials.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { app ->
                    Text(
                        app.label,
                        color = look.color,
                        fontFamily = look.textFamily,
                        fontSize = size,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (columns == 2) 14.dp else 0.dp)
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.Button, onClickLabel = "Open ${app.label}") { onOpen(app) }
                            .padding(vertical = 9.dp),
                    )
                }
                if (row.size < columns) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyHome(look: HomeLook, onOpenSettings: () -> Unit) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(
            "Nothing pinned yet",
            color = look.color,
            fontFamily = look.textFamily,
            fontSize = 22.sp,
        )
        Text(
            "Choose the few apps you truly need. Everything else waits behind the wall.",
            color = look.color.copy(alpha = 0.75f),
            fontFamily = look.textFamily,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        TextButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 4.dp)) {
            Text("Choose apps", color = look.color)
        }
    }
}

@Composable
private fun HomeFooter(look: HomeLook, onOpenWall: () -> Unit, onOpenSettings: () -> Unit) {
    val tone = look.color.copy(alpha = 0.78f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClickLabel = "Open everything else", onClick = onOpenWall)
                .padding(end = 12.dp),
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = tone, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Everything else", color = tone, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = tone, modifier = Modifier.size(20.dp))
        }
    }
}

/** A quiet card for one-off prompts on the home screen (default launcher, first run). */
@Composable
private fun HomeNotice(look: HomeLook, title: String, body: String, action: String, onAction: () -> Unit) {
    val light = look.color.isLight()
    val ground = if (light) Color.Black.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.55f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(ground)
            .clickable(role = Role.Button, onClick = onAction)
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = look.color, style = MaterialTheme.typography.titleSmall)
            Text(
                body,
                color = look.color.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        TextButton(onClick = onAction) { Text(action, color = if (light) Sage else SageDeep) }
    }
}

@Composable
fun WallpaperLayer(settings: LauncherSettings, modifier: Modifier = Modifier.fillMaxSize()) {
    val context = LocalContext.current
    when (settings.wallpaperMode) {
        WallpaperMode.SYSTEM -> Unit // the system wallpaper shows through our transparent window
        WallpaperMode.COLOR -> Box(
            modifier.background(
                Brush.verticalGradient(listOf(Color(settings.wallpaperColor), Color(settings.wallpaperColor2)))
            )
        )
        WallpaperMode.PHOTO -> {
            val bitmap by produceState<ImageBitmap?>(null, settings.wallpaperVersion) {
                value = withContext(Dispatchers.IO) { Media.loadWallpaper(context)?.asImageBitmap() }
            }
            Box(modifier.background(Ink))
            bitmap?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
            }
        }
    }
    if (settings.dim > 0f) {
        Box(modifier.background(Color.Black.copy(alpha = settings.dim)))
    }
}

/** Current time, ticking each second (or each minute). Reading it only recomposes the caller. */
@Composable
fun rememberNow(minuteOnly: Boolean = false): LocalDateTime {
    LocalFixedNow.current?.let { return it }
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            val ms = System.currentTimeMillis()
            delay(if (minuteOnly) 60_000L - ms % 60_000L else 1000L - ms % 1000L)
        }
    }
    return now
}

@Composable
fun ClockView(style: ClockStyle, use24h: Boolean, look: HomeLook, userScale: Float) {
    // Digits are corrected per font; the analog face has no type, so it keeps the user's size.
    val scale = if (style == ClockStyle.ANALOG) userScale else userScale * look.clockSize
    val now = rememberNow(minuteOnly = style != ClockStyle.ANALOG)
    val hour12 = (now.hour % 12).let { if (it == 0) 12 else it }
    val hh = if (use24h) "%02d".format(now.hour) else hour12.toString()
    val mm = "%02d".format(now.minute)
    val ampm = if (use24h) "" else if (now.hour < 12) "am" else "pm"
    val color = look.color
    val family = look.clockFamily
    val weight = look.clockWeight

    when (style) {
        ClockStyle.DIGITAL -> Row {
            Text(
                "$hh:$mm",
                modifier = Modifier.alignByBaseline(),
                maxLines = 1,
                softWrap = false,
                color = color,
                fontFamily = family,
                fontWeight = weight,
                fontSize = (92 * scale).sp,
                letterSpacing = (-2 * scale).sp,
                style = TextStyle(
                    lineHeight = (92 * scale).sp,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
            )
            if (ampm.isNotEmpty()) {
                Text(
                    ampm,
                    color = color.copy(alpha = 0.7f),
                    fontFamily = family,
                    fontWeight = weight,
                    fontSize = (22 * scale).sp,
                    modifier = Modifier.alignByBaseline().padding(start = 8.dp),
                )
            }
        }
        ClockStyle.STACKED -> {
            // One two-line paragraph: the gap is set by lineHeight alone, not by each font's padding.
            val stacked = buildAnnotatedString {
                append(hh.padStart(2, '0'))
                append("\n")
                withStyle(SpanStyle(color = color.copy(alpha = 0.5f))) { append(mm) }
            }
            Text(
                stacked,
                color = color,
                fontFamily = family,
                fontWeight = weight,
                fontSize = (120 * scale).sp,
                letterSpacing = (-3 * scale).sp,
                style = TextStyle(
                    lineHeight = (108 * scale).sp,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
            )
        }
        ClockStyle.WORDS -> Text(
            "it's " + timeInWords(now.hour, now.minute),
            color = color,
            fontFamily = family,
            fontWeight = weight,
            fontSize = (44 * scale).sp,
            lineHeight = (50 * scale).sp,
            letterSpacing = (-0.5 * scale).sp,
        )
        ClockStyle.ANALOG -> AnalogClock(now, color, (216 * scale).dp)
        ClockStyle.MINIMAL -> Text(
            "$hh:$mm $ampm".trim(),
            color = color,
            fontFamily = family,
            fontWeight = weight,
            fontSize = (30 * scale).sp,
            letterSpacing = (3 * scale).sp,
        )
    }
}

@Composable
fun AnalogClock(now: LocalDateTime, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(color.copy(alpha = 0.22f), radius = r, center = c, style = Stroke(width = 1.5.dp.toPx()))
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90.0)
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            val hourMark = i % 5 == 0
            drawLine(
                color = color.copy(alpha = if (hourMark) 0.75f else 0.25f),
                start = c + dir * (r * if (hourMark) 0.84f else 0.9f),
                end = c + dir * (r * 0.95f),
                strokeWidth = if (hourMark) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        fun hand(degrees: Double, length: Float, width: Float, handColor: Color) {
            val a = Math.toRadians(degrees - 90.0)
            drawLine(
                color = handColor,
                start = c,
                end = c + Offset(cos(a).toFloat(), sin(a).toFloat()) * length,
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }
        hand((now.hour % 12 + now.minute / 60.0) * 30.0, r * 0.5f, 5.dp.toPx(), color)
        hand((now.minute + now.second / 60.0) * 6.0, r * 0.76f, 3.dp.toPx(), color)
        hand(now.second * 6.0, r * 0.86f, 1.2.dp.toPx(), Sage)
        drawCircle(color, radius = 4.dp.toPx(), center = c)
        drawCircle(Sage, radius = 1.8.dp.toPx(), center = c)
    }
}

/** Fuzzy "word clock", rounded to the nearest five minutes. */
fun timeInWords(hour: Int, minute: Int): String {
    val names = listOf(
        "twelve", "one", "two", "three", "four", "five", "six",
        "seven", "eight", "nine", "ten", "eleven",
    )
    val rounded = ((minute + 2) / 5) * 5
    val current = names[hour % 12]
    val next = names[(hour + 1) % 12]
    fun minuteWord(m: Int) = when (m) {
        5 -> "five"; 10 -> "ten"; 20 -> "twenty"; 25 -> "twenty-five"; else -> m.toString()
    }
    return when (rounded) {
        0 -> "$current o'clock"
        60 -> "$next o'clock"
        15 -> "quarter past $current"
        30 -> "half past $current"
        45 -> "quarter to $next"
        in 5..25 -> "${minuteWord(rounded)} past $current"
        else -> "${minuteWord(60 - rounded)} to $next"
    }
}
