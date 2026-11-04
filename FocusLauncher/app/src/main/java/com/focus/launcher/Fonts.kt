package com.focus.launcher

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Fonts offered for the clock and the home-screen text.
 *
 * Bundled fonts are SIL Open Font License 1.1 or Apache 2.0 (licences in assets/licenses).
 * [size] corrects for how big a font *looks*: handwriting such as Caveat draws far smaller than
 * a sans at the same point size, so it is scaled up to match.
 */
enum class FontChoice(
    val label: String,
    @FontRes val res: Int?,
    val minWeight: Int,
    val maxWeight: Int,
    val size: Float,
    /** Android system font family name, for the phone's own fonts. */
    val system: String? = null,
) {
    // ---- handwriting & script
    CURSIVE("Cursive", null, 400, 400, 1.22f, system = "cursive"),
    CASUAL("Casual", null, 400, 400, 1.0f, system = "casual"),
    CAVEAT("Caveat", R.font.caveat, 400, 700, 1.3f),
    PACIFICO("Pacifico", R.font.pacifico, 400, 400, 0.95f),
    SATISFY("Satisfy", R.font.satisfy, 400, 400, 1.18f),
    SHADOWS("Shadows", R.font.shadows_into_light, 400, 400, 1.18f),
    INDIE_FLOWER("Indie Flower", R.font.indie_flower, 400, 400, 1.05f),
    GLORIA("Gloria", R.font.gloria, 400, 400, 0.95f),
    PERMANENT_MARKER("Marker", R.font.permanent_marker, 400, 400, 1.0f),
    AMATIC("Amatic", R.font.amatic_sc, 400, 400, 1.4f),

    // ---- display
    LOBSTER("Lobster", R.font.lobster, 400, 400, 1.05f),
    ABRIL("Abril", R.font.abril_fatface, 400, 400, 1.0f),
    BEBAS_NEUE("Bebas Neue", R.font.bebas_neue, 400, 400, 1.15f),
    RIGHTEOUS("Righteous", R.font.righteous, 400, 400, 1.0f),
    MONOTON("Monoton", R.font.monoton, 400, 400, 0.85f),
    BUNGEE("Bungee", R.font.bungee, 400, 400, 0.85f),
    ORBITRON("Orbitron", R.font.orbitron, 400, 900, 0.9f),
    PRESS_START("Pixel", R.font.press_start, 400, 400, 0.62f),
    MAJOR_MONO("Major Mono", R.font.major_mono, 400, 400, 0.85f),
    POIRET("Poiret", R.font.poiret_one, 400, 400, 1.1f),
    COMFORTAA("Comfortaa", R.font.comfortaa, 300, 700, 1.0f),
    SYNE("Syne", R.font.syne, 400, 800, 1.0f),

    // ---- serif
    INSTRUMENT_SERIF("Instrument", R.font.instrument_serif, 400, 400, 1.15f),
    FRAUNCES("Fraunces", R.font.fraunces, 100, 900, 1.0f),
    PLAYFAIR("Playfair", R.font.playfair_display, 400, 900, 1.0f),
    CINZEL("Cinzel", R.font.cinzel, 400, 900, 0.92f),

    // ---- clean
    OUTFIT("Outfit", R.font.outfit, 100, 900, 1.0f),
    MANROPE("Manrope", R.font.manrope, 200, 800, 1.0f),
    SYSTEM("System", null, 100, 900, 1.0f, system = "sans-serif"),
    CUSTOM("Imported", null, 400, 400, 1.0f);

    val hasWeights: Boolean get() = maxWeight > minWeight

    fun clamp(weight: Int): Int = weight.coerceIn(minWeight, maxWeight)

    companion object {
        /** Reads a stored name, including names from earlier versions of the app. */
        fun parse(name: String?, fallback: FontChoice): FontChoice =
            entries.firstOrNull { it.name == name } ?: when (name) {
                "MONO", "JETBRAINS_MONO" -> MAJOR_MONO
                "SERIF", "DM_SERIF" -> INSTRUMENT_SERIF
                "DEFAULT", "LIGHT", "THIN", "CONDENSED", "SORA", "SPACE_GROTESK" -> OUTFIT
                else -> fallback
            }
    }
}

/** Named weights offered for variable fonts. */
val FontWeightSteps = listOf(
    100 to "Hairline", 200 to "Thin", 300 to "Light", 400 to "Regular", 500 to "Medium", 700 to "Bold", 800 to "Heavy",
)

object Fonts {
    private val cache = HashMap<String, FontFamily>()

    /** The UI face: Manrope at the four weights the type scale uses. */
    val Ui: FontFamily = FontFamily(
        Font(R.font.manrope, FontWeight.Normal),
        Font(R.font.manrope, FontWeight.Medium),
        Font(R.font.manrope, FontWeight.SemiBold),
        Font(R.font.manrope, FontWeight.Bold),
    )

    /** Family for [choice] rendered at [weight]; variable fonts get a matching instance. */
    fun family(context: Context, choice: FontChoice, weight: Int, customVersion: Int = 0): FontFamily {
        val w = choice.clamp(weight)
        val key = "${choice.name}:$w:$customVersion"
        cache[key]?.let { return it }
        val family = when {
            choice == FontChoice.SYSTEM -> FontFamily.SansSerif
            choice == FontChoice.CUSTOM -> Media.loadFont(context)?.let { FontFamily(it) } ?: FontFamily.SansSerif
            choice.system != null -> FontFamily(Typeface.create(choice.system, Typeface.NORMAL))
            choice.hasWeights -> FontFamily(Font(choice.res!!, FontWeight(w)))
            else -> FontFamily(Font(choice.res!!))
        }
        cache[key] = family
        return family
    }

    /** The weight to request alongside [family] so no synthetic bolding happens. */
    fun weight(choice: FontChoice, weight: Int): FontWeight = when {
        choice == FontChoice.SYSTEM -> FontWeight(choice.clamp(weight))
        choice.system != null -> FontWeight.Normal
        choice.hasWeights -> FontWeight(choice.clamp(weight))
        else -> FontWeight.Normal
    }
}
