package com.prettycountdown.widget

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.prettycountdown.MainActivity
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.util.CountdownBreakdown
import com.prettycountdown.util.CountdownMath

/** Everything a style renderer needs to draw the countdown itself. */
data class CountdownDisplay(
    val big: String,
    val unit: String,
    val progress: Float,
    val breakdown: CountdownBreakdown,
)

internal fun computeDisplay(event: Event, format: CountdownFormat): CountdownDisplay {
    val now = System.currentTimeMillis()
    val breakdown = CountdownMath.breakdown(event.targetDateTime, now)
    val progress = CountdownMath.progressFraction(event.startDateTime, event.targetDateTime, now)
    return CountdownDisplay(
        big = CountdownMath.primaryValue(format, breakdown, progress),
        unit = CountdownMath.unitLabel(format, breakdown),
        progress = progress,
        breakdown = breakdown,
    )
}

/** Tapping any countdown widget opens the app to that event's detail screen. */
@Composable
internal fun openEventAction(eventId: Long) =
    actionStartActivity(
        Intent(LocalContext.current, MainActivity::class.java),
        actionParametersOf(WidgetActionParams.EVENT_ID to eventId),
    )

/** Headline number size for the given widget size, shared across every style. */
internal fun bigFontSize(size: WidgetSizeBucket): TextUnit = when (size) {
    WidgetSizeBucket.SMALL -> 32.sp
    WidgetSizeBucket.MEDIUM -> 40.sp
    WidgetSizeBucket.LARGE -> 56.sp
}

/** Picks the right style renderer for an event and dispatches to it. */
@Composable
fun WidgetContent(
    event: Event,
    style: WidgetStyle,
    format: CountdownFormat,
    photoBitmap: Bitmap?,
    sizeBucket: WidgetSizeBucket,
) {
    val display = computeDisplay(event, format)
    val palette = ColorPalettes.byKey(event.colorPaletteKey)
    val modifier = GlanceModifier.fillMaxSize().clickable(openEventAction(event.id))

    when (style) {
        WidgetStyle.MINIMAL -> MinimalWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.ELEGANT -> ElegantWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.FLIP_CLOCK -> FlipClockWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.CIRCULAR_PROGRESS -> CircularProgressWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.GLASSMORPHISM -> GlassmorphismWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.KAWAII -> KawaiiWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.RETRO_PIXEL -> RetroPixelWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.NEON -> NeonWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.CALENDAR -> CalendarWidget(event, display, palette, sizeBucket, modifier)
        WidgetStyle.PHOTO -> PhotoWidget(event, display, palette, photoBitmap, sizeBucket, modifier)
        WidgetStyle.JOURNEY -> JourneyWidget(event, display, palette, sizeBucket, modifier)
    }
}

/** Shown when the widget has no event to display yet (none created, or all deleted). */
@Composable
fun EmptyStateWidget() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Tap to create your first countdown 🎉",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        )
    }
}
