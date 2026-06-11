package com.prettycountdown.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prettycountdown.data.model.ColorPalette
import com.prettycountdown.data.model.Event
import java.time.Instant
import java.time.ZoneId

/** Big number, clean typography, nothing else. */
@Composable
fun MinimalWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    Column(
        modifier = modifier.background(ColorProvider(Color(palette.background))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (size != WidgetSizeBucket.SMALL) {
            Text(
                event.name,
                style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
        }
        Text(display.big, style = TextStyle(color = ColorProvider(Color(palette.primary)), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold))
        Text(display.unit, style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 14.sp))
    }
}

/** Soft serif vibes with a category icon and an italic subtitle. */
@Composable
fun ElegantWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    Column(
        modifier = modifier.background(ColorProvider(Color(palette.background))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(event.category.emoji, style = TextStyle(fontSize = 22.sp))
        Text(display.big, style = TextStyle(color = ColorProvider(Color(palette.primary)), fontSize = bigFontSize(size), fontWeight = FontWeight.Normal))
        Text(display.unit, style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 13.sp, fontStyle = FontStyle.Italic))
        if (size == WidgetSizeBucket.LARGE) {
            Spacer(GlanceModifier.height(8.dp))
            Text(event.name, style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 14.sp), maxLines = 1)
        }
    }
}

/** Retro split-flap digits on a dark board. */
@Composable
fun FlipClockWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    Column(
        modifier = modifier.background(ColorProvider(Color(0xFF1B1B1F))).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (size != WidgetSizeBucket.SMALL) {
            Text(event.name, style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp), maxLines = 1)
            Spacer(GlanceModifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            display.big.forEachIndexed { index, digit ->
                if (index > 0) Spacer(GlanceModifier.width(4.dp))
                FlipDigit(digit, size)
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(display.unit, style = TextStyle(color = ColorProvider(Color(0xFFFFC857)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun FlipDigit(digit: Char, size: WidgetSizeBucket) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(Color(0xFF2E2E35)))
            .cornerRadius(6.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            digit.toString(),
            style = TextStyle(color = ColorProvider(Color.White), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
        )
    }
}

/** A ring of dots that fills as the date nears, plus the headline number. */
@Composable
fun CircularProgressWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    val segments = if (size == WidgetSizeBucket.SMALL) 5 else 8
    Column(
        modifier = modifier.background(ColorProvider(Color(palette.background))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (size != WidgetSizeBucket.SMALL) {
            Text(event.name, style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Spacer(GlanceModifier.height(4.dp))
        }
        Text(WidgetVisuals.progressDots(display.progress, segments), style = TextStyle(color = ColorProvider(Color(palette.primary)), fontSize = 16.sp))
        Spacer(GlanceModifier.height(8.dp))
        Text(display.big, style = TextStyle(color = ColorProvider(Color(palette.primary)), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold))
        Text(display.unit, style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 13.sp))
    }
}

/** Frosted glass card floating over a color wash. */
@Composable
fun GlassmorphismWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    Box(
        modifier = modifier.background(ColorProvider(Color(palette.primary))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier
                .padding(16.dp)
                .background(ColorProvider(Color.White.copy(alpha = 0.25f)))
                .cornerRadius(20.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (size != WidgetSizeBucket.SMALL) {
                Text(event.name, style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
            }
            Text(display.big, style = TextStyle(color = ColorProvider(Color.White), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold))
            Text(display.unit, style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp))
        }
    }
}

/** Pastel colors, friendly emoji, sleeps left. */
@Composable
fun KawaiiWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    Column(
        modifier = modifier.background(ColorProvider(Color(0xFFFFF0F5))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${event.category.emoji} ✨", style = TextStyle(fontSize = 18.sp))
        Text(display.big, style = TextStyle(color = ColorProvider(Color(0xFFFF6FB5)), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold))
        Text("${display.unit} 🌙", style = TextStyle(color = ColorProvider(Color(0xFF5C1F36)), fontSize = 13.sp))
        if (size == WidgetSizeBucket.LARGE) {
            Spacer(GlanceModifier.height(6.dp))
            Text(event.name, style = TextStyle(color = ColorProvider(Color(0xFF5C1F36)), fontSize = 13.sp), maxLines = 1)
        }
    }
}

/** Monospace, bracketed, 8-bit energy. */
@Composable
fun RetroPixelWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    val green = ColorProvider(Color(0xFF39FF14))
    Column(
        modifier = modifier.background(ColorProvider(Color(0xFF0D0D0D))).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (size != WidgetSizeBucket.SMALL) {
            Text("> ${event.name}", style = TextStyle(color = green, fontSize = 12.sp, fontFamily = FontFamily.Monospace), maxLines = 1)
            Spacer(GlanceModifier.height(6.dp))
        }
        Text("[ ${display.big} ]", style = TextStyle(color = green, fontSize = bigFontSize(size), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
        Text(display.unit.uppercase(), style = TextStyle(color = green, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
    }
}

/** Glowing digits on a midnight background. */
@Composable
fun NeonWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    Column(
        modifier = modifier.background(ColorProvider(Color(0xFF0F0F1A))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(event.category.emoji, style = TextStyle(fontSize = 18.sp))
        Text(display.big, style = TextStyle(color = ColorProvider(Color(0xFFB14EFF)), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold))
        Text(display.unit, style = TextStyle(color = ColorProvider(Color(0xFF6EE7FF)), fontSize = 13.sp, fontWeight = FontWeight.Medium))
        if (size != WidgetSizeBucket.SMALL) {
            Spacer(GlanceModifier.height(4.dp))
            Text(event.name, style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp), maxLines = 1)
        }
    }
}

/** A torn calendar page for the big day. */
@Composable
fun CalendarWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    val date = Instant.ofEpochMilli(event.targetDateTime).atZone(ZoneId.systemDefault())
    Column(
        modifier = modifier.background(ColorProvider(Color.White)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(date.month.name.take(3), style = TextStyle(color = ColorProvider(Color(0xFFE63946)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
        Text(date.dayOfMonth.toString(), style = TextStyle(color = ColorProvider(Color(0xFF1A1A1A)), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(4.dp))
        Text("${display.big} ${display.unit}", style = TextStyle(color = ColorProvider(Color(palette.primary)), fontSize = 13.sp, fontWeight = FontWeight.Medium))
        if (size == WidgetSizeBucket.LARGE) {
            Spacer(GlanceModifier.height(4.dp))
            Text(event.name, style = TextStyle(color = ColorProvider(Color(0xFF555555)), fontSize = 12.sp), maxLines = 1)
        }
    }
}

/** The user's photo fills the background with a countdown overlay. */
@Composable
fun PhotoWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, photoBitmap: Bitmap?, size: WidgetSizeBucket, modifier: GlanceModifier) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomStart) {
        if (photoBitmap != null) {
            Image(
                provider = ImageProvider(photoBitmap),
                contentDescription = event.name,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        } else {
            Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(palette.background)))) {}
        }
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color.Black.copy(alpha = 0.45f)))
                .padding(12.dp),
        ) {
            if (size != WidgetSizeBucket.SMALL) {
                Text(event.name, style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
            Text(
                "${display.big} ${display.unit}",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = if (size == WidgetSizeBucket.SMALL) 18.sp else 22.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

/** A little traveler walks toward the date. */
@Composable
fun JourneyWidget(event: Event, display: CountdownDisplay, palette: ColorPalette, size: WidgetSizeBucket, modifier: GlanceModifier) {
    val trackLength = when (size) {
        WidgetSizeBucket.SMALL -> 5
        WidgetSizeBucket.MEDIUM -> 9
        WidgetSizeBucket.LARGE -> 12
    }
    Column(
        modifier = modifier.background(ColorProvider(Color(palette.background))).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (size != WidgetSizeBucket.SMALL) {
            Text(event.name, style = TextStyle(color = ColorProvider(Color(palette.onBackground)), fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Spacer(GlanceModifier.height(6.dp))
        }
        Text(
            WidgetVisuals.journeyTrack(display.progress, event.category.emoji, trackLength),
            style = TextStyle(fontSize = if (size == WidgetSizeBucket.SMALL) 14.sp else 18.sp),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text("${display.big} ${display.unit}", style = TextStyle(color = ColorProvider(Color(palette.primary)), fontSize = bigFontSize(size), fontWeight = FontWeight.Bold))
    }
}
