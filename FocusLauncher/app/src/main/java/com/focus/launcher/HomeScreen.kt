package com.focus.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.font.FontFamily
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
    val family = rememberClockFont(settings)
    val textColor = Color(settings.textColor)
    val now = rememberNow()
    val essentials = settings.essentials.mapNotNull { key -> apps.firstOrNull { it.key == key } }
    val openWall by rememberUpdatedState(onOpenWall)
    val openSettings by rememberUpdatedState(onOpenSettings)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { openSettings() }) }
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -150f) openWall() },
                    onVerticalDrag = { _, dy -> dragged += dy },
                )
            }
    ) {
        WallpaperLayer(settings)

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            if (!isDefault) {
                MakeDefaultBanner(textColor, onMakeDefault)
            }
            Spacer(Modifier.height(36.dp))
            ClockView(settings.clockStyle, settings.use24h, family, textColor, settings.clockScale, now)

            if (settings.showDate) {
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    color = textColor.copy(alpha = 0.75f),
                    fontFamily = family,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (settings.homeNote.isNotBlank()) {
                Text(
                    settings.homeNote,
                    color = textColor.copy(alpha = 0.9f),
                    fontFamily = family,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            val stats = buildList {
                if (screenTimeMs != null) add("${Usage.format(screenTimeMs)} on screen today")
                if (settings.resistedToday > 0) add("${settings.resistedToday} distraction${if (settings.resistedToday == 1) "" else "s"} skipped")
            }
            if (stats.isNotEmpty()) {
                Text(
                    stats.joinToString("  ·  "),
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            essentials.forEach { app ->
                Text(
                    app.label,
                    color = textColor,
                    fontFamily = family,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(app) }
                        .padding(vertical = 10.dp),
                )
            }
            if (essentials.isEmpty() && apps.isNotEmpty()) {
                Text(
                    "Long-press anywhere to choose your essential apps",
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "↑  everything else",
                    color = textColor.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onOpenWall() }.padding(vertical = 8.dp),
                )
                Text(
                    "settings",
                    color = textColor.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onOpenSettings() }.padding(vertical = 8.dp),
                )
            }
        }
    }
}

/** Shown until Focus Launcher is the phone's default home app. */
@Composable
fun MakeDefaultBanner(textColor: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Make this your home screen", color = textColor, fontSize = 15.sp)
            Text(
                "So the Home button brings you here, not to your old icons.",
                color = textColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }
        Text("Set", color = Sage, fontSize = 15.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun WallpaperLayer(settings: LauncherSettings) {
    val context = LocalContext.current
    when (settings.wallpaperMode) {
        WallpaperMode.SYSTEM -> Unit // the system wallpaper shows through our transparent window
        WallpaperMode.COLOR -> Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(settings.wallpaperColor), Color(settings.wallpaperColor2)))
            )
        )
        WallpaperMode.PHOTO -> {
            val bitmap by produceState<ImageBitmap?>(null, settings.wallpaperVersion) {
                value = withContext(Dispatchers.IO) { Media.loadWallpaper(context)?.asImageBitmap() }
            }
            Box(Modifier.fillMaxSize().background(Color.Black))
            bitmap?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
    if (settings.dim > 0f) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = settings.dim)))
    }
}

@Composable
fun rememberClockFont(settings: LauncherSettings): FontFamily {
    val context = LocalContext.current
    val choice = settings.fontChoice
    val version = settings.fontVersion
    return remember(choice, version) { Media.fontFamilyFor(context, choice) }
}

@Composable
fun rememberNow(): LocalDateTime {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    return now
}

@Composable
fun ClockView(
    style: ClockStyle,
    use24h: Boolean,
    family: FontFamily,
    color: Color,
    scale: Float,
    now: LocalDateTime,
) {
    val hour12 = (now.hour % 12).let { if (it == 0) 12 else it }
    val hh = if (use24h) "%02d".format(now.hour) else hour12.toString()
    val mm = "%02d".format(now.minute)
    val ampm = if (use24h) "" else if (now.hour < 12) " am" else " pm"

    when (style) {
        ClockStyle.DIGITAL -> Text(
            "$hh:$mm",
            color = color,
            fontFamily = family,
            fontSize = (88 * scale).sp,
            lineHeight = (92 * scale).sp,
        )
        ClockStyle.STACKED -> Column {
            Text(hh.padStart(2, '0'), color = color, fontFamily = family, fontSize = (112 * scale).sp, lineHeight = (104 * scale).sp)
            Text(mm, color = color.copy(alpha = 0.55f), fontFamily = family, fontSize = (112 * scale).sp, lineHeight = (104 * scale).sp)
        }
        ClockStyle.WORDS -> Text(
            "it's\n" + timeInWords(now.hour, now.minute),
            color = color,
            fontFamily = family,
            fontSize = (42 * scale).sp,
            lineHeight = (48 * scale).sp,
        )
        ClockStyle.ANALOG -> AnalogClock(now, color, (210 * scale).dp)
        ClockStyle.MINIMAL -> Text(
            "$hh:$mm$ampm",
            color = color,
            fontFamily = family,
            fontSize = (30 * scale).sp,
            letterSpacing = 4.sp,
        )
    }
}

@Composable
fun AnalogClock(now: LocalDateTime, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(color.copy(alpha = 0.25f), radius = r, center = c, style = Stroke(width = 2.dp.toPx()))
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = c + dir * (r * 0.84f),
                end = c + dir * (r * 0.94f),
                strokeWidth = 2.dp.toPx(),
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
        hand((now.hour % 12 + now.minute / 60.0) * 30.0, r * 0.5f, 6.dp.toPx(), color)
        hand((now.minute + now.second / 60.0) * 6.0, r * 0.74f, 4.dp.toPx(), color)
        hand(now.second * 6.0, r * 0.84f, 1.5.dp.toPx(), Sage)
        drawCircle(color, radius = 5.dp.toPx(), center = c)
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
