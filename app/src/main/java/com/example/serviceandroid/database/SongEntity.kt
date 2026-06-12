package com.example.serviceandroid.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.serviceandroid.model.Song

@Entity(tableName = "songEntity")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val nameSinger: String,
    val thumbnailUrl: String,
    val audioUrl: String,
    val lyricUrl: String,
    val durationSec: Long,
    val categoryId: String,
    val categoryName: String,
    var timeCreate: String? = null,
) {
    constructor(song: Song, timeCreate: String?) : this(
        id = song.id,
        title = song.title,
        nameSinger = song.nameSinger,
        thumbnailUrl = song.thumbnailUrl,
        audioUrl = song.audioUrl,
        lyricUrl = song.lyricUrl,
        durationSec = song.durationSec,
        categoryId = song.categoryId,
        categoryName = song.categoryName,
        timeCreate = timeCreate,
    )
}
