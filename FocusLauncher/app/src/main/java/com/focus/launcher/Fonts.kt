package com.focus.launcher

import android.content.Context
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Fonts bundled with the app (all SIL Open Font License 1.1, licences in assets/licenses).
 * Variable fonts expose a weight range; static fonts have min == max.
 */
enum class FontChoice(
    val label: String,
    @FontRes val res: Int?,
    val minWeight: Int,
    val maxWeight: Int,
) {
    OUTFIT("Outfit", R.font.outfit, 100, 900),
    MANROPE("Manrope", R.font.manrope, 200, 800),
    SORA("Sora", R.font.sora, 100, 800),
    SPACE_GROTESK("Space Grotesk", R.font.space_grotesk, 300, 700),
    SYNE("Syne", R.font.syne, 400, 800),
    BEBAS_NEUE("Bebas Neue", R.font.bebas_neue, 400, 400),
    INSTRUMENT_SERIF("Instrument Serif", R.font.instrument_serif, 400, 400),
    DM_SERIF("DM Serif", R.font.dm_serif_display, 400, 400),
    PLAYFAIR("Playfair", R.font.playfair_display, 400, 900),
    FRAUNCES("Fraunces", R.font.fraunces, 100, 900),
    JETBRAINS_MONO("JetBrains Mono", R.font.jetbrains_mono, 100, 800),
    MAJOR_MONO("Major Mono", R.font.major_mono, 400, 400),
    CAVEAT("Caveat", R.font.caveat, 400, 700),
    SYSTEM("System", null, 100, 900),
    CUSTOM("Imported", null, 400, 400);

    val hasWeights: Boolean get() = maxWeight > minWeight

    fun clamp(weight: Int): Int = weight.coerceIn(minWeight, maxWeight)

    companion object {
        /** Reads a stored name, including names from the first version of the app. */
        fun parse(name: String?, fallback: FontChoice): FontChoice =
            entries.firstOrNull { it.name == name } ?: when (name) {
                "MONO" -> JETBRAINS_MONO
                "SERIF" -> INSTRUMENT_SERIF
                "CURSIVE", "CASUAL" -> CAVEAT
                "DEFAULT", "LIGHT", "THIN", "CONDENSED" -> OUTFIT
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
        val family = when (choice) {
            FontChoice.SYSTEM -> FontFamily.SansSerif
            FontChoice.CUSTOM -> Media.loadFont(context)?.let { FontFamily(it) } ?: FontFamily.SansSerif
            else -> if (choice.hasWeights) {
                FontFamily(Font(choice.res!!, FontWeight(w)))
            } else {
                FontFamily(Font(choice.res!!))
            }
        }
        cache[key] = family
        return family
    }

    /** The weight to request alongside [family] so no synthetic bolding happens. */
    fun weight(choice: FontChoice, weight: Int): FontWeight =
        if (choice.hasWeights) FontWeight(choice.clamp(weight)) else FontWeight.Normal
}
