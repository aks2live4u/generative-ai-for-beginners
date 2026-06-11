package com.prettycountdown.ui.widgetgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.widget.WidgetVisuals

private val previewPalette = ColorPalettes.byKey("ocean")
private const val SAMPLE_BIG = "42"
private const val SAMPLE_UNIT = "Days Left"
private const val SAMPLE_NAME = "Summer Trip"
private const val SAMPLE_EMOJI = "✈️"
private const val SAMPLE_PROGRESS = 0.45f

/**
 * A miniature recreation of [style] using sample data, drawn with plain Compose so the
 * gallery can show "what you'll actually get" without needing a live Glance widget.
 */
@Composable
fun WidgetStylePreview(style: WidgetStyle, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        when (style) {
            WidgetStyle.MINIMAL -> MinimalPreview()
            WidgetStyle.ELEGANT -> ElegantPreview()
            WidgetStyle.FLIP_CLOCK -> FlipClockPreview()
            WidgetStyle.CIRCULAR_PROGRESS -> CircularProgressPreview()
            WidgetStyle.GLASSMORPHISM -> GlassmorphismPreview()
            WidgetStyle.KAWAII -> KawaiiPreview()
            WidgetStyle.RETRO_PIXEL -> RetroPixelPreview()
            WidgetStyle.NEON -> NeonPreview()
            WidgetStyle.CALENDAR -> CalendarPreview()
            WidgetStyle.PHOTO -> PhotoPreview()
            WidgetStyle.JOURNEY -> JourneyPreview()
        }
    }
}

@Composable
private fun MinimalPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(previewPalette.background)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(SAMPLE_NAME, color = Color(previewPalette.onBackground), fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(SAMPLE_BIG, color = Color(previewPalette.primary), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(SAMPLE_UNIT, color = Color(previewPalette.onBackground), fontSize = 9.sp)
    }
}

@Composable
private fun ElegantPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(previewPalette.background)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(SAMPLE_EMOJI, fontSize = 14.sp)
        Text(SAMPLE_BIG, color = Color(previewPalette.primary), fontSize = 26.sp, fontWeight = FontWeight.Normal)
        Text(SAMPLE_UNIT, color = Color(previewPalette.onBackground), fontSize = 9.sp, fontStyle = FontStyle.Italic)
        Text(SAMPLE_NAME, color = Color(previewPalette.onBackground), fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun FlipClockPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1B1B1F)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(SAMPLE_NAME, color = Color.White, fontSize = 8.sp, maxLines = 1)
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            SAMPLE_BIG.forEachIndexed { index, digit ->
                if (index > 0) Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier.background(Color(0xFF2E2E35), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(digit.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(SAMPLE_UNIT.uppercase(), color = Color(0xFFFFC857), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CircularProgressPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(previewPalette.background)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(SAMPLE_NAME, color = Color(previewPalette.onBackground), fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text(WidgetVisuals.progressDots(SAMPLE_PROGRESS, 8), color = Color(previewPalette.primary), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(SAMPLE_BIG, color = Color(previewPalette.primary), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(SAMPLE_UNIT, color = Color(previewPalette.onBackground), fontSize = 9.sp)
    }
}

@Composable
private fun GlassmorphismPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(previewPalette.primary)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(SAMPLE_NAME, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(SAMPLE_BIG, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(SAMPLE_UNIT, color = Color.White, fontSize = 9.sp)
        }
    }
}

@Composable
private fun KawaiiPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0F5)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$SAMPLE_EMOJI ✨", fontSize = 13.sp)
        Text(SAMPLE_BIG, color = Color(0xFFFF6FB5), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("$SAMPLE_UNIT 🌙", color = Color(0xFF5C1F36), fontSize = 9.sp)
    }
}

@Composable
private fun RetroPixelPreview() {
    val green = Color(0xFF39FF14)
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("> $SAMPLE_NAME", color = green, fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        Text("[ $SAMPLE_BIG ]", color = green, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(SAMPLE_UNIT.uppercase(), color = green, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun NeonPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F1A)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(SAMPLE_EMOJI, fontSize = 13.sp)
        Text(SAMPLE_BIG, color = Color(0xFFB14EFF), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(SAMPLE_UNIT, color = Color(0xFF6EE7FF), fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Text(SAMPLE_NAME, color = Color.White, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun CalendarPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("JUN", color = Color(0xFFE63946), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("21", color = Color(0xFF1A1A1A), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("$SAMPLE_BIG $SAMPLE_UNIT", color = Color(previewPalette.primary), fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Text(SAMPLE_NAME, color = Color(0xFF555555), fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun PhotoPreview() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(previewPalette.primary), Color(previewPalette.secondary))))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(8.dp),
        ) {
            Text(SAMPLE_NAME, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("$SAMPLE_BIG $SAMPLE_UNIT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun JourneyPreview() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(previewPalette.background)).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(SAMPLE_NAME, color = Color(previewPalette.onBackground), fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text(WidgetVisuals.journeyTrack(SAMPLE_PROGRESS, SAMPLE_EMOJI, 6), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text("$SAMPLE_BIG $SAMPLE_UNIT", color = Color(previewPalette.primary), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
