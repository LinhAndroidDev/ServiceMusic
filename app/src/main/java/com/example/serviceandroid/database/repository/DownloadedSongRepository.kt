package com.example.serviceandroid.database.repository

import android.content.Context
import com.example.serviceandroid.database.DownloadStatus
import com.example.serviceandroid.database.DownloadedSongEntity
import com.example.serviceandroid.database.dao.DownloadedSongDao
import com.example.serviceandroid.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DownloadedSongRepository @Inject constructor(
    private val dao: DownloadedSongDao,
    @ApplicationContext private val context: Context,
) {
    fun observeCompletedSongs(): Flow<List<Song>> =
        dao.observeByStatus(DownloadStatus.COMPLETED).map { list ->
            list.map { it.toSong() }
        }

    fun observeCompletedCount(): Flow<Int> =
        dao.observeCountByStatus(DownloadStatus.COMPLETED)

    fun observeStatus(songId: String): Flow<DownloadStatus?> =
        dao.observeById(songId).map { it?.status }

    suspend fun getById(songId: String): DownloadedSongEntity? = dao.getById(songId)

    suspend fun enqueueMetadata(song: Song) {
        val existing = dao.getById(song.id)
        if (existing?.status == DownloadStatus.COMPLETED &&
            existing.localAudioPath.isNotBlank() &&
            File(existing.localAudioPath).exists()
        ) {
            return
        }
        dao.upsert(DownloadedSongEntity.fromSong(song, DownloadStatus.QUEUED))
    }

    suspend fun markDownloading(songId: String) {
        dao.updateStatus(songId, DownloadStatus.DOWNLOADING)
    }

    suspend fun markCompleted(songId: String, localPath: String) {
        dao.updateDownloadResult(
            songId = songId,
            status = DownloadStatus.COMPLETED,
            localAudioPath = localPath,
            downloadedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markFailed(songId: String) {
        dao.updateStatus(songId, DownloadStatus.FAILED)
    }

    suspend fun deleteDownload(songId: String) {
        val entity = dao.getById(songId)
        entity?.localAudioPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
        audioDir().listFiles()
            ?.filter { it.name.startsWith(songId) }
            ?.forEach { runCatching { it.delete() } }
        dao.deleteById(songId)
    }

    /** Local file URI string if completed and file exists; otherwise null. */
    suspend fun resolveLocalPlayableUri(songId: String): String? {
        val entity = dao.getById(songId) ?: return null
        if (entity.status != DownloadStatus.COMPLETED) return null
        val path = entity.localAudioPath
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return null
        return file.toURI().toString()
    }

    fun audioDir(): File {
        return File(context.filesDir, "downloads/audio").also { it.mkdirs() }
    }

    fun tempFileFor(songId: String): File = File(audioDir(), "$songId.part")

    fun finalFileFor(songId: String, remoteUrl: String): File {
        val ext = remoteUrl.substringAfterLast('.', "mp3")
            .substringBefore('?')
            .ifBlank { "mp3" }
            .take(5)
        return File(audioDir(), "$songId.$ext")
    }
}
