package com.example.serviceandroid.lyrics

import com.example.serviceandroid.database.repository.DownloadedSongRepository
import com.example.serviceandroid.model.Song
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongLyricsLoader @Inject constructor(
    private val downloadedSongRepository: DownloadedSongRepository,
) {

    /**
     * Loads lyrics preferring a local offline file, then [Song.lyricUrl] (LRC/.txt).
     * Returns `null` if missing / IO error / empty parse.
     */
    suspend fun loadTimedLines(song: Song): List<TimedLyricLine>? {
        val text = try {
            val localPath = downloadedSongRepository.resolveLocalLyricPath(song.id)
            when {
                localPath != null -> File(localPath).readText()
                song.lyricUrl.isNotBlank() -> downloadText(song.lyricUrl.trim())
                else -> return null
            }
        } catch (_: Exception) {
            return null
        }
        val parsed = LrcLineParser.parse(text)
        return parsed.ifEmpty { null }
    }

    private fun downloadText(urlString: String): String {
        if (urlString.startsWith("file:", ignoreCase = true)) {
            return File(URL(urlString).toURI()).readText()
        }
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
