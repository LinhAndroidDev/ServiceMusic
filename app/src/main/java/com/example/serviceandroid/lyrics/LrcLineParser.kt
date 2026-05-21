package com.example.serviceandroid.lyrics

/**
 * Parses LRC-like lines: `[mm:ss.SS] lyric text` (centiseconds after dot).
 * Lines that do not match or have empty text after the tag are skipped.
 */
object LrcLineParser {

    private val lineRegex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2})]\s*(.*)$""")

    fun parse(fileText: String): List<TimedLyricLine> {
        val out = ArrayList<TimedLyricLine>()
        fileText.lineSequence().forEach { raw ->
            val line = raw.trimEnd('\r', ' ')
            val m = lineRegex.matchEntire(line) ?: return@forEach
            val mm = m.groupValues[1].toInt()
            val ss = m.groupValues[2].toInt()
            val centi = m.groupValues[3].toInt()
            val text = m.groupValues[4].trim()
            if (text.isEmpty()) return@forEach
            val startSec = mm * 60.0 + ss + centi / 100.0
            out.add(TimedLyricLine(startSec = startSec, text = text))
        }
        return out
    }
}
