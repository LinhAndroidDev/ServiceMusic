package com.example.serviceandroid.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.serviceandroid.model.Song

@Entity(tableName = "downloaded_song")
data class DownloadedSongEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val nameSinger: String,
    val thumbnailUrl: String,
    val remoteAudioUrl: String,
    val lyricUrl: String,
    val durationSec: Long,
    val categoryId: String,
    val categoryName: String,
    val localAudioPath: String = "",
    val localLyricPath: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val downloadedAt: Long = 0L,
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        nameSinger = nameSinger,
        thumbnailUrl = thumbnailUrl,
        audioUrl = remoteAudioUrl,
        lyricUrl = lyricUrl,
        durationSec = durationSec,
        categoryId = categoryId,
        categoryName = categoryName,
    )

    companion object {
        fun fromSong(song: Song, status: DownloadStatus = DownloadStatus.QUEUED): DownloadedSongEntity =
            DownloadedSongEntity(
                songId = song.id,
                title = song.title,
                nameSinger = song.nameSinger,
                thumbnailUrl = song.thumbnailUrl,
                remoteAudioUrl = song.audioUrl,
                lyricUrl = song.lyricUrl,
                durationSec = song.durationSec,
                categoryId = song.categoryId,
                categoryName = song.categoryName,
                status = status,
            )
    }
}
