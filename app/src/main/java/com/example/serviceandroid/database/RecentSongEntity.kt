package com.example.serviceandroid.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.serviceandroid.model.Song

@Entity(tableName = "recent_song")
data class RecentSongEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val nameSinger: String,
    val thumbnailUrl: String,
    val audioUrl: String,
    val lyricUrl: String,
    val durationSec: Long,
    val categoryId: String,
    val categoryName: String,
    val views: Long,
    val lastPlayedAt: Long,
    val playCount: Long,
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        nameSinger = nameSinger,
        thumbnailUrl = thumbnailUrl,
        audioUrl = audioUrl,
        lyricUrl = lyricUrl,
        durationSec = durationSec,
        categoryId = categoryId,
        categoryName = categoryName,
        views = views,
    )

    companion object {
        fun fromSong(
            song: Song,
            lastPlayedAt: Long = System.currentTimeMillis(),
            playCount: Long = 1,
        ): RecentSongEntity = RecentSongEntity(
            songId = song.id,
            title = song.title,
            nameSinger = song.nameSinger,
            thumbnailUrl = song.thumbnailUrl,
            audioUrl = song.audioUrl,
            lyricUrl = song.lyricUrl,
            durationSec = song.durationSec,
            categoryId = song.categoryId,
            categoryName = song.categoryName,
            views = song.views,
            lastPlayedAt = lastPlayedAt,
            playCount = playCount,
        )
    }
}
