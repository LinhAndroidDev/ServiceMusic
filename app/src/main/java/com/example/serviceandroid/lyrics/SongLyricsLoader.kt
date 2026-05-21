package com.example.serviceandroid.lyrics

import android.content.Context
import com.example.serviceandroid.model.Song
import java.io.IOException

object SongLyricsLoader {

    private const val ASSET_DIR = "lyrics"

    /**
     * Loads `assets/lyrics/{rawName}.txt` (LRC-style lines). Returns `null` if file missing / IO error.
     * Returns empty list if file exists but no valid lines parsed.
     */
    fun loadTimedLines(context: Context, song: Song): List<TimedLyricLine>? {
        val entry = try {
            context.resources.getResourceEntryName(song.sing)
        } catch (_: Exception) {
            return null
        }
        val path = "$ASSET_DIR/$entry.txt"
        val text = try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            return null
        }
        return LrcLineParser.parse(text)
    }
}
