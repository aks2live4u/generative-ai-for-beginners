package com.aivideotranscriber.export

import com.aivideotranscriber.whisper.TranscriptSegment
import java.util.Locale

object TranscriptExporter {

    fun toPlainText(segments: List<TranscriptSegment>, includeTimestamps: Boolean): String =
        segments.joinToString("\n\n") { seg ->
            if (includeTimestamps) "[${formatClock(seg.startMs)}] ${seg.text}" else seg.text
        }

    fun toSrt(segments: List<TranscriptSegment>): String {
        val sb = StringBuilder()
        segments.forEachIndexed { index, seg ->
            sb.append(index + 1).append('\n')
            sb.append(formatSrtTime(seg.startMs)).append(" --> ").append(formatSrtTime(seg.endMs)).append('\n')
            sb.append(seg.text.trim()).append('\n').append('\n')
        }
        return sb.toString()
    }

    fun formatClock(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s, millis)
    }
}
