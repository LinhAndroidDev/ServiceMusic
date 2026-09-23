package com.example.serviceandroid.download

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.serviceandroid.database.repository.DownloadedSongRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class SongDownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadedSongRepository: DownloadedSongRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val songId = inputData.getString(KEY_SONG_ID).orEmpty()
        if (songId.isBlank()) return Result.failure()

        val entity = downloadedSongRepository.getById(songId)
            ?: return Result.failure()
        val remoteUrl = entity.remoteAudioUrl
        if (remoteUrl.isBlank()) {
            downloadedSongRepository.markFailed(songId)
            return Result.failure()
        }

        downloadedSongRepository.markDownloading(songId)

        val tempFile = downloadedSongRepository.tempFileFor(songId)
        val finalFile = downloadedSongRepository.finalFileFor(songId, remoteUrl)
        runCatching { if (tempFile.exists()) tempFile.delete() }

        return try {
            downloadToFile(remoteUrl, tempFile)
            if (tempFile.length() <= 0L) {
                throw IllegalStateException("Empty download for $songId")
            }
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val localLyricPath = downloadLyricBestEffort(songId, entity.lyricUrl)

            downloadedSongRepository.markCompleted(
                songId = songId,
                localAudioPath = finalFile.absolutePath,
                localLyricPath = localLyricPath,
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed songId=$songId", e)
            runCatching { tempFile.delete() }
            downloadedSongRepository.markFailed(songId)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Lyric is optional: audio download still succeeds if lyric fetch fails.
     * @return absolute path when saved, otherwise empty string.
     */
    private fun downloadLyricBestEffort(songId: String, lyricUrl: String): String {
        val url = lyricUrl.trim()
        if (url.isBlank()) return ""
        val tempLyric = downloadedSongRepository.tempLyricFileFor(songId)
        val finalLyric = downloadedSongRepository.finalLyricFileFor(songId)
        return try {
            runCatching { if (tempLyric.exists()) tempLyric.delete() }
            downloadToFile(url, tempLyric)
            if (tempLyric.length() <= 0L) {
                tempLyric.delete()
                return ""
            }
            if (finalLyric.exists()) finalLyric.delete()
            if (!tempLyric.renameTo(finalLyric)) {
                tempLyric.copyTo(finalLyric, overwrite = true)
                tempLyric.delete()
            }
            finalLyric.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Lyric download skipped songId=$songId", e)
            runCatching { tempLyric.delete() }
            ""
        }
    }

    private fun downloadToFile(remoteUrl: String, dest: File) {
        val connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code for $remoteUrl")
            }
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        private const val TAG = "SongDownloadWorker"
        private const val MAX_RETRIES = 5
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}
