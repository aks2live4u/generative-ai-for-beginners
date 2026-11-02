package com.nexus.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.theme.Nx
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Obsidian backdrop: a faint cyan horizon glow, a dot grid and slow drifting motes. */
@Composable
fun NexusBackground(reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val motes = remember {
        val r = Random(2035)
        List(38) { Triple(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 1.6f) }
    }
    val drift = if (reducedMotion) 0f else {
        val t = rememberInfiniteTransition(label = "bg")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "drift").value
    }
    Canvas(modifier.fillMaxSize().background(Nx.Obsidian)) {
        drawRect(
            Brush.radialGradient(
                listOf(Color(0x2238E8FF), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 1.05f),
                radius = size.width * 1.1f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color(0x1438E8FF), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.08f),
                radius = size.width * 0.7f,
            ),
        )
        val step = 28.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(Color(0x0F9AF5FF), radius = 0.9f, center = Offset(x, y))
                x += step
            }
            y += step
        }
        motes.forEach { (mx, my, s) ->
            val yy = ((my - drift * s * 0.25f) % 1f + 1f) % 1f
            drawCircle(Nx.Cyan.copy(alpha = 0.10f + 0.12f * (s / 2f)), radius = s * 1.4f, center = Offset(mx * size.width, yy * size.height))
        }
    }
}

/** Frosted obsidian panel with a hairline cyan edge. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color = Nx.Cyan,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Nx.SurfaceHi.copy(alpha = 0.92f), Nx.Surface.copy(alpha = 0.86f))))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.06f))), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun HudLabel(text: String, modifier: Modifier = Modifier, color: Color = Nx.Muted) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** A small pulsing status light. */
@Composable
fun StatusDot(color: Color = Nx.Mint, reducedMotion: Boolean = false) {
    val a = if (reducedMotion) 1f else rememberInfiniteTransition(label = "dot")
        .animateFloat(0.35f, 1f, infiniteRepeatable(tween(1_400), RepeatMode.Reverse), label = "a").value
    Box(Modifier.size(7.dp).alpha(a).clip(CircleShape).background(color))
}

enum class OrbMode { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * The NEXUS core: a glowing sphere inside counter-rotating orbital arcs.
 * Listening makes it breathe with the voice level, thinking spins the arcs faster.
 */
@Composable
fun NexusOrb(
    mode: OrbMode,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    level: Float = 0f,
    reducedMotion: Boolean = false,
) {
    val t = rememberInfiniteTransition(label = "orb")
    val speed = when (mode) { OrbMode.THINKING -> 2_400; OrbMode.LISTENING -> 5_000; else -> 14_000 }
    val spin = if (reducedMotion) 0f else
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(speed, easing = LinearEasing)), label = "spin").value
    val breathe = if (reducedMotion) 0.5f else
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(2_600), RepeatMode.Reverse), label = "breathe").value
    val voice by animateFloatAsState(if (mode == OrbMode.LISTENING) level else 0f, tween(90), label = "voice")
    val heat = when (mode) { OrbMode.IDLE -> 0.55f; OrbMode.SPEAKING -> 0.9f; else -> 0.8f }

    Canvas(modifier.size(size)) {
        val c = center
        val r = this.size.minDimension / 2f
        val core = r * (0.34f + 0.03f * breathe + 0.10f * voice)
        drawCircle(
            Brush.radialGradient(listOf(Nx.Cyan.copy(alpha = 0.30f * heat), Color.Transparent), c, r),
            radius = r, center = c,
        )
        drawCircle(
            Brush.radialGradient(listOf(Color.White, Nx.CyanSoft, Nx.Cyan.copy(alpha = 0.55f), Color.Transparent), c, core * 1.35f),
            radius = core * 1.35f, center = c,
        )
        val arcs = listOf(Triple(0.62f, 1f, 110f), Triple(0.76f, -0.7f, 70f), Triple(0.90f, 0.45f, 160f))
        arcs.forEachIndexed { i, (rf, dir, sweep) ->
            val rr = r * rf
            rotate(spin * dir + i * 50f, c) {
                drawArc(
                    Nx.Cyan.copy(alpha = 0.85f - i * 0.2f), startAngle = 0f, sweepAngle = sweep, useCenter = false,
                    topLeft = Offset(c.x - rr, c.y - rr), size = androidx.compose.ui.geometry.Size(rr * 2, rr * 2),
                    style = Stroke(width = (2.4f - i * 0.5f).dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    Nx.Cyan.copy(alpha = 0.25f), startAngle = sweep + 30f, sweepAngle = 40f, useCenter = false,
                    topLeft = Offset(c.x - rr, c.y - rr), size = androidx.compose.ui.geometry.Size(rr * 2, rr * 2),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        val n = 48
        for (k in 0 until n) {
            val a = (k.toFloat() / n) * 2 * PI.toFloat() + spin * PI.toFloat() / 360f
            val wobble = if (mode == OrbMode.LISTENING) voice * 0.12f * sin(k * 1.7f + spin / 20f) else 0f
            val rr = r * (0.98f + wobble)
            drawCircle(
                Nx.CyanSoft.copy(alpha = if (k % 4 == 0) 0.7f else 0.25f),
                radius = if (k % 4 == 0) 1.8f else 1f,
                center = Offset(c.x + rr * cos(a), c.y + rr * sin(a)),
            )
        }
    }
}

/** App icon in a dark capsule with a cyan rim. */
@Composable
fun AppGlyph(icon: ImageBitmap, label: String, size: Dp = 56.dp, dimmed: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .alpha(if (dimmed) 0.28f else 1f)
            .clip(CircleShape)
            .background(Nx.SurfaceHi)
            .border(1.dp, Nx.Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(icon, contentDescription = label, modifier = Modifier.size(size * 0.72f))
    }
}

/** Ring gauge used by battery and focus widgets. */
@Composable
fun RingGauge(progress: Float, color: Color, modifier: Modifier = Modifier, stroke: Dp = 6.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Canvas(modifier) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
        drawArc(Nx.CyanDim, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(color, -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            HudLabel(title, color = Nx.Cyan)
            if (subtitle != null) {
                Spacer(Modifier.size(4.dp))
                Text(subtitle, style = MaterialTheme.typography.headlineMedium)
            }
        }
        trailing?.invoke()
    }
}

/** A chip-like pill button, at least 48dp tall for easy tapping. */
@Composable
fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    accent: Color = Nx.Cyan,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.18f) else Nx.SurfaceHi.copy(alpha = 0.8f))
            .border(1.dp, if (selected) accent.copy(alpha = 0.8f) else Nx.Line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = if (selected) Nx.Text else Nx.Text.copy(alpha = 0.85f))
    }
}

@Composable
fun SettingRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Nx.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Nx.Obsidian, checkedTrackColor = Nx.Cyan,
                uncheckedThumbColor = Nx.Muted, uncheckedTrackColor = Nx.SurfaceHi, uncheckedBorderColor = Nx.Line,
            ),
        )
    }
}
