package com.dosemate.data

data class ParsedMedicine(
    val name: String,
    val dosage: String,
    val frequency: Frequency,
    val timesOfDay: List<Pair<Int, Int>>,
    val everyXHours: Int = 0
)

object PrescriptionParser {

    private val DOSAGE_RE = Regex(
        """(\d+(?:\.\d+)?)\s*(mg|mcg|µg|ml|mL|g\b|iu\b|IU\b|units?|tablets?|tabs?|caps?|capsules?)""",
        RegexOption.IGNORE_CASE
    )

    private val SKIP_LINE_RE = Regex(
        """^\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}|^dr\.?|^patient|^rx|^refills?|^sig:|^qty|^diagnosis|^composition|^timing""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    private val NAME_PREFIX_RE = Regex(
        """^\s*\d+[.)]\s*|^\s*[-•*]\s*|^\s*(tab|cap|syp|inj|susp|oint|gel|cream|drops?|sol|oral)\s*\.?\s+""",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun parse(ocrText: String): List<ParsedMedicine> {
        val allLines = ocrText.lines().map { it.trim() }
        val result = mutableListOf<ParsedMedicine>()
        val seen = mutableSetOf<String>()

        for (i in allLines.indices) {
            val line = allLines[i]
            if (line.length <= 4 || SKIP_LINE_RE.containsMatchIn(line)) continue
            // Look ahead 3 raw lines for timing data (table columns often land on adjacent lines)
            val lookahead = allLines.drop(i + 1).take(3).joinToString(" ")
            val medicine = parseLine(line, lookahead) ?: continue
            val key = medicine.name.lowercase()
            if (key !in seen) {
                seen.add(key)
                result.add(medicine)
            }
        }
        return result
    }

    private fun parseLine(line: String, lookahead: String = ""): ParsedMedicine? {
        val dosageMatch = DOSAGE_RE.find(line) ?: return null
        val rawName = line.substring(0, dosageMatch.range.first).trim()
            .replace(NAME_PREFIX_RE, "")
            .trim()
            .trimEnd('.', ',', ':', '-', '/')
            .trim()

        if (rawName.length < 2 || rawName.all { it.isDigit() || it == '/' || it == '-' }) return null
        // Skip composition sub-lines regardless of how OCR prefixes them (e.g. "N Composition :")
        if (rawName.contains("composition", ignoreCase = true)) return null

        val name = rawName.split(Regex("\\s+")).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercaseChar() }
        }

        // Combine text after dosage on this line with the next few lines so the M-A-N
        // pattern (which HealthPlix prints in a separate table column) is also searched
        val after = (line.substring(dosageMatch.range.last + 1) + " " + lookahead).lowercase()
        val (freq, times, xHours) = parseFrequency(after)

        return ParsedMedicine(
            name = name,
            dosage = dosageMatch.value.trim(),
            frequency = freq,
            timesOfDay = times,
            everyXHours = xHours
        )
    }

    private data class FreqResult(
        val frequency: Frequency,
        val times: List<Pair<Int, Int>>,
        val xHours: Int = 0
    )

    private fun parseFrequency(text: String): FreqResult {
        if (text.containsAny("sos", "prn", "as needed", "as required", "when required", "when necessary")) {
            return FreqResult(Frequency.SOS, emptyList())
        }

        val xHoursMatch = Regex("""every\s+(\d+)\s*h(?:ours?)?|(\d+)\s*hourly""").find(text)
        if (xHoursMatch != null) {
            val x = (xHoursMatch.groupValues[1].toIntOrNull() ?: xHoursMatch.groupValues[2].toIntOrNull() ?: 8)
                .coerceIn(1, 24)
            return FreqResult(Frequency.EVERY_X_HOURS, listOf(8 to 0), x)
        }

        if (text.containsAny("alternate day", "every other day", "eod", "alt day", "on alternate")) {
            return FreqResult(Frequency.ALTERNATE_DAYS, listOf(8 to 0))
        }

        // M-A-E-N pattern (4 positions: morning-afternoon-evening-night), e.g. "1-0-0-1"
        val maenMatch = Regex("""(?<![.\d])([0-9])\s*[-–]\s*([0-9])\s*[-–]\s*([0-9])\s*[-–]\s*([0-9])(?!\d)""").find(text)
        if (maenMatch != null) {
            val (m, a, e, n) = listOf(1, 2, 3, 4).map { maenMatch.groupValues[it].toIntOrNull() ?: 0 }
            val times = buildList {
                if (m > 0) add(8 to 0)
                if (a > 0) add(14 to 0)
                if (e > 0) add(18 to 0)
                if (n > 0) add(21 to 0)
            }
            if (times.isNotEmpty()) return FreqResult(Frequency.DAILY, times)
        }

        // M-A-N pattern (3 positions: morning-afternoon-night), e.g. "1-0-1" or "1-1-1"
        val manMatch = Regex("""(?<![.\d])([0-9])\s*[-–]\s*([0-9])\s*[-–]\s*([0-9])(?!\d)""").find(text)
        if (manMatch != null) {
            val (m, a, n) = listOf(1, 2, 3).map { manMatch.groupValues[it].toIntOrNull() ?: 0 }
            val times = buildList {
                if (m > 0) add(8 to 0)
                if (a > 0) add(14 to 0)
                if (n > 0) add(21 to 0)
            }
            if (times.isNotEmpty()) return FreqResult(Frequency.DAILY, times)
        }

        if (text.containsAny("qds", "qid", "4 times", "four times", "4x daily")) {
            return FreqResult(Frequency.DAILY, listOf(8 to 0, 12 to 0, 16 to 0, 20 to 0))
        }

        if (text.containsAny("tds", "tid", "thrice", "3 times", "three times", "three-time", "3x daily")) {
            return FreqResult(Frequency.DAILY, listOf(8 to 0, 14 to 0, 21 to 0))
        }

        if (text.containsAny("bd", "bid", "twice", "2 times", "two times", "2x daily", "12 hourly")) {
            return FreqResult(Frequency.DAILY, listOf(8 to 0, 21 to 0))
        }

        // Once daily — infer time from keywords
        val time = when {
            text.containsAny("night", "bedtime", " hs", "nocte", "nightly", "before sleep", "at bedtime",
                "after dinner", "post dinner", "after supper") -> 21 to 0
            text.containsAny("evening", "dinner time") -> 18 to 0
            text.containsAny("afternoon", "noon", "lunch", "midday", "after lunch") -> 14 to 0
            else -> 8 to 0  // morning default
        }
        return FreqResult(Frequency.DAILY, listOf(time))
    }

    private fun String.containsAny(vararg keywords: String): Boolean = keywords.any { contains(it) }
}
