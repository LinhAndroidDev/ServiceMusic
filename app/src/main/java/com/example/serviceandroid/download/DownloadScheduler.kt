package com.example.serviceandroid.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.serviceandroid.database.DownloadStatus
import com.example.serviceandroid.database.repository.DownloadedSongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedSongRepository: DownloadedSongRepository,
) {
    suspend fun enqueue(song: Song) {
        downloadedSongRepository.enqueueMetadata(song)
        val existing = downloadedSongRepository.getById(song.id)
        if (existing?.status == DownloadStatus.COMPLETED &&
            existing.localAudioPath.isNotBlank() &&
            File(existing.localAudioPath).exists()
        ) {
            return
        }

        val request = OneTimeWorkRequestBuilder<SongDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(workDataOf(SongDownloadWorker.KEY_SONG_ID to song.id))
            .addTag(WORK_TAG)
            .addTag(workNameFor(song.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workNameFor(song.id),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(songId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workNameFor(songId))
    }

    companion object {
        const val WORK_TAG = "song_download"
        fun workNameFor(songId: String) = "download_song_$songId"
    }
}
