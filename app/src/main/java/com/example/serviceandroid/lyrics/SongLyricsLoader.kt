package com.example.serviceandroid.lyrics

import com.example.serviceandroid.model.Song
import java.net.HttpURLConnection
import java.net.URL

object SongLyricsLoader {

    /**
     * Loads lyrics from [Song.lyricUrl] (LRC-style). Returns `null` if URL missing / IO error.
     */
    suspend fun loadTimedLines(song: Song): List<TimedLyricLine>? {
        val url = song.lyricUrl.trim()
        if (url.isBlank()) return null
        val text = try {
            downloadText(url)
        } catch (_: Exception) {
            return null
        }
        val parsed = LrcLineParser.parse(text)
        return parsed.ifEmpty { null }
    }

    private fun downloadText(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
        }
        return conn.inputStream.bufferedReader().use { it.readText() }.also {
            conn.disconnect()
        }
    }
}
