package com.focus.launcher

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- tokens

// Night terrace sky. Hierarchy is magnitude (size and brightness), not boxes.
/** Ink-indigo night ground. */
val Ink = Color(0xFF0A0D16)
val Surface1 = Color(0xFF111626)
val Surface2 = Color(0xFF181E31)
val Surface3 = Color(0xFF232A40)
/** Coordinate hairlines. */
val Hairline = Color(0xFF2A3149)
/** Chalk-white star: primary text. */
val Star = Color(0xFFECEFF7)
/** A dimmer magnitude for secondary text: 7:1 on Ink. */
val Muted = Color(0xFF9AA3B9)
/** The faintest magnitude: decoration and disabled. */
val Faint = Color(0xFF5D6681)
/** Moonlight: the calm accent (go, done, chosen). */
val Moon = Color(0xFFC3D3F7)
val MoonDeep = Color(0xFF1E2A47)
/** Night-vision ember: cost, limits, late hours. Kind to dark-adapted eyes. */
val Ember = Color(0xFFF08A63)

private fun TextStyle.ui() = copy(fontFamily = Fonts.Ui)

private val base = Typography()
private val FocusTypography = Typography(
    displayLarge = base.displayLarge.ui().copy(fontWeight = FontWeight.Normal),
    displayMedium = base.displayMedium.ui(),
    displaySmall = base.displaySmall.ui(),
    headlineLarge = base.headlineLarge.ui().copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.ui().copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineSmall = base.headlineSmall.ui().copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = base.titleLarge.ui().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.ui().copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.ui().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.ui().copy(lineHeight = 25.sp, letterSpacing = 0.2.sp),
    bodyMedium = base.bodyMedium.ui().copy(lineHeight = 21.sp, letterSpacing = 0.2.sp),
    bodySmall = base.bodySmall.ui().copy(lineHeight = 17.sp, letterSpacing = 0.25.sp),
    labelLarge = base.labelLarge.ui().copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = base.labelMedium.ui().copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.ui().copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun FocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Star,
            onPrimary = Ink,
            primaryContainer = Surface3,
            onPrimaryContainer = Star,
            secondary = Moon,
            onSecondary = Ink,
            secondaryContainer = MoonDeep,
            onSecondaryContainer = Moon,
            tertiary = Ember,
            background = Ink,
            onBackground = Star,
            surface = Ink,
            onSurface = Star,
            surfaceVariant = Surface2,
            onSurfaceVariant = Muted,
            surfaceContainerLowest = Ink,
            surfaceContainerLow = Surface1,
            surfaceContainer = Surface1,
            surfaceContainerHigh = Surface2,
            surfaceContainerHighest = Surface3,
            surfaceBright = Surface3,
            surfaceDim = Ink,
            outline = Color(0xFF57534C),
            outlineVariant = Hairline,
            error = Ember,
            onError = Ink,
        ),
        typography = FocusTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
    ) {
        // Screens draw their own backgrounds instead of using Surface, so set the default
        // content colour here; without it, icons without an explicit tint render black.
        CompositionLocalProvider(LocalContentColor provides Star, content = content)
    }
}

/** True when [color] needs dark system-bar icons / dark text on top of it. */
fun Color.isLight(): Boolean = luminance() > 0.45f

// ---------------------------------------------------------------- motion

/** Honour the system "Remove animations" setting. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** A countdown that can be extended while it runs. [total] grows with it, so progress stays honest. */
@Stable
class Countdown(initial: Int) {
    var remaining by mutableIntStateOf(initial)
    var total by mutableIntStateOf(initial.coerceAtLeast(1))
    val done: Boolean get() = remaining <= 0
    /** 0 at the start, 1 when the wait is over. */
    val progress: Float get() = if (total <= 0) 1f else 1f - remaining.toFloat() / total
    fun add(seconds: Int) {
        remaining += seconds
        total += seconds
    }
}

@Composable
fun rememberCountdown(seconds: Int): Countdown {
    val countdown = remember { Countdown(seconds) }
    // One tick per change: the effect restarts whenever the value moves, and stops at zero.
    LaunchedEffect(countdown.remaining) {
        if (countdown.remaining > 0) {
            delay(1000)
            countdown.remaining--
        }
    }
    return countdown
}

private const val BREATH_MS = 4000

/**
 * The signature of the pause: a moon that waxes from new to full as the wait runs down. A faint
 * halo breathes in and out on a four-second rhythm so there is something to breathe along with.
 */
@Composable
fun MoonPhase(countdown: Countdown, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "breath")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathHalo",
    )
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(BREATH_MS * 2, easing = LinearEasing), RepeatMode.Restart),
        label = "breathCycle",
    )
    val inhale = cycle < 1f
    val phase by animateFloatAsState(countdown.progress, tween(900, easing = FastOutSlowInEasing), label = "moonPhase")
    val done = countdown.done
    val halo = if (reduceMotion) 0.5f else breath

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(196.dp)
                .semantics { contentDescription = if (done) "Ready" else "${countdown.remaining} seconds left" }
                .drawBehind {
                    val r = size.minDimension * 0.34f
                    val c = center
                    // Halo: moonlight scattering in night air, swelling with each breath in.
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Moon.copy(alpha = 0.10f + 0.10f * halo * phase.coerceAtLeast(0.2f)), Color.Transparent),
                            center = c,
                            radius = r * (1.55f + 0.25f * halo),
                        ),
                        radius = r * (1.55f + 0.25f * halo),
                        center = c,
                    )
                    // The unlit moon.
                    drawCircle(Surface3, radius = r, center = c)
                    // The lit part: right half plus/minus a terminator ellipse.
                    val p = phase.coerceIn(0f, 1f)
                    if (p > 0.001f) {
                        val half = Path().apply {
                            addArc(Rect(c.x - r, c.y - r, c.x + r, c.y + r), -90f, 180f)
                            close()
                        }
                        val rx = r * kotlin.math.abs(1f - 2f * p)
                        val ellipse = Path().apply { addOval(Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)) }
                        val lit = Path()
                        lit.op(half, ellipse, if (p < 0.5f) PathOperation.Difference else PathOperation.Union)
                        drawPath(lit, Brush.linearGradient(listOf(Color(0xFFF4F1E6), Moon), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)))
                    }
                    drawCircle(Star.copy(alpha = 0.18f), radius = r, center = c, style = Stroke(1.dp.toPx()))
                },
        )
        Text(
            if (done) "Your call" else countdown.remaining.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
            color = if (done) Moon else Star,
        )
        Text(
            if (done) "Take one more breath, then choose" else if (inhale) "Breathe in" else "Breathe out",
            style = MaterialTheme.typography.labelLarge,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Today's opens as stars: one lit star per open, and with a daily limit, the remaining ones as
 * empty sockets. The cost is visible at a glance, without a number to argue with.
 */
@Composable
fun StarRow(opens: Int, limit: Int, modifier: Modifier = Modifier) {
    val shown = if (limit > 0) limit else (opens + 1).coerceAtMost(12)
    val description = if (limit > 0) "$opens of $limit opens used today" else "$opens opens today"
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        repeat(shown) { i ->
            val used = i < opens
            val isNext = i == opens
            Canvas(Modifier.size(10.dp)) {
                when {
                    used -> {
                        drawCircle(Ember.copy(alpha = 0.25f), radius = size.minDimension * 0.75f)
                        drawCircle(Ember, radius = size.minDimension * 0.38f)
                    }
                    isNext -> drawCircle(Star, radius = size.minDimension * 0.38f, style = Stroke(1.2.dp.toPx()))
                    else -> drawCircle(Faint, radius = size.minDimension * 0.3f, style = Stroke(1.dp.toPx()))
                }
            }
        }
        if (limit <= 0 && opens >= 12) {
            Text("+${opens - 11}", style = MaterialTheme.typography.labelMedium, color = Ember)
        }
    }
}

// ---------------------------------------------------------------- pickers

/** Searchable checklist of apps. [keyOf] decides whether selection is by component key or package. */
@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppEntry>,
    initiallySelected: Set<String>,
    keyOf: (AppEntry) -> String,
    maxSelected: Int = Int.MAX_VALUE,
    onDismiss: () -> Unit,
    onDone: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initiallySelected) }
    val shown = apps.filter { it.label.contains(query.trim(), ignoreCase = true) }.distinctBy(keyOf)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (maxSelected == Int.MAX_VALUE) "${selected.size} selected"
                    else "${selected.size} of $maxSelected selected",
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                LazyColumn(Modifier.height(360.dp)) {
                    items(shown, key = { keyOf(it) }) { app ->
                        val key = keyOf(app)
                        val checked = key in selected
                        val canAdd = checked || selected.size < maxSelected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable(enabled = canAdd, role = Role.Checkbox) {
                                    selected = if (checked) selected - key else selected + key
                                },
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, enabled = canAdd, modifier = Modifier.padding(horizontal = 8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (app.kind.isDistracting) {
                                    Text(app.kind.tag, color = Muted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(selected) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A round colour (or gradient) chip with a 48dp touch target. */
@Composable
fun Swatch(colors: List<Color>, selected: Boolean, description: String, onClick: () -> Unit) {
    val brush = if (colors.size > 1) Brush.verticalGradient(colors) else Brush.linearGradient(listOf(colors[0], colors[0]))
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(brush)
                .border(if (selected) 2.dp else 1.dp, if (selected) Star else Color(0x40FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                val tint = if (colors.first().isLight()) Ink else Star
                Icon(Icons.Filled.Check, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** One tile of a font gallery: a sample set in the font, its name underneath. */
@Composable
fun FontTile(
    choice: FontChoice,
    sample: String,
    weight: Int,
    customVersion: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val family = remember(choice, weight, customVersion) { Fonts.family(context, choice, weight, customVersion) }
    Column(
        Modifier
            .width(112.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) Surface3 else Surface1)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Star else Hairline, MaterialTheme.shapes.medium)
            .clickable(role = Role.RadioButton, onClickLabel = "Use ${choice.label}", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(
            sample,
            fontFamily = family,
            fontWeight = Fonts.weight(choice, weight),
            fontSize = (30 * choice.size.coerceAtMost(1.25f)).sp,
            lineHeight = 40.sp,
            color = Star,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Text(
            choice.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Star else Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
