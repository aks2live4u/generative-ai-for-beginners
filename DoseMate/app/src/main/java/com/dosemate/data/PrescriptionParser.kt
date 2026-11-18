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
        """^\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}|^dr\.?|^patient|^rx|^refills?|^sig:|^qty|^diagnosis""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    private val NAME_PREFIX_RE = Regex(
        """^\s*\d+[.)]\s*|^\s*[-•*]\s*|^\s*(tab|cap|syp|inj|susp|oint|gel|cream|drops?|sol|oral)\s*\.?\s+""",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun parse(ocrText: String): List<ParsedMedicine> =
        ocrText.lines()
            .map { it.trim() }
            .filter { it.length > 4 && !SKIP_LINE_RE.containsMatchIn(it) }
            .mapNotNull(::parseLine)
            .distinctBy { it.name.lowercase() }

    private fun parseLine(line: String): ParsedMedicine? {
        val dosageMatch = DOSAGE_RE.find(line) ?: return null
        val rawName = line.substring(0, dosageMatch.range.first).trim()
            .replace(NAME_PREFIX_RE, "")
            .trim()
            .trimEnd('.', ',', ':', '-', '/')
            .trim()

        if (rawName.length < 2 || rawName.all { it.isDigit() || it == '/' || it == '-' }) return null

        val name = rawName.split(Regex("\\s+")).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercaseChar() }
        }

        val after = line.substring(dosageMatch.range.last + 1).lowercase()
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
            return FreqResult(Frequency.EVERY_X_HOURS, listOf(9 to 0), x)
        }

        if (text.containsAny("alternate day", "every other day", "eod", "alt day", "on alternate")) {
            return FreqResult(Frequency.ALTERNATE_DAYS, listOf(9 to 0))
        }

        if (text.containsAny("qds", "qid", "4 times", "four times", "4x daily")) {
            return FreqResult(Frequency.DAILY, listOf(8 to 0, 12 to 0, 16 to 0, 20 to 0))
        }

        if (text.containsAny("tds", "tid", "thrice", "3 times", "three times", "three-time", "3x daily")) {
            return FreqResult(Frequency.DAILY, listOf(9 to 0, 14 to 0, 21 to 0))
        }

        if (text.containsAny("bd", "bid", "twice", "2 times", "two times", "2x daily", "12 hourly")) {
            return FreqResult(Frequency.DAILY, listOf(9 to 0, 21 to 0))
        }

        // Default: once daily
        return FreqResult(Frequency.DAILY, listOf(9 to 0))
    }

    private fun String.containsAny(vararg keywords: String): Boolean = keywords.any { contains(it) }
}
