package com.example.serviceandroid.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.serviceandroid.model.Song

@Entity(tableName = "songEntity")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val idSong: Int,
    val title: String,
    val nameSinger: String,
    val avatar: Int,
    val sing: Int,
    val time: Int,
    val type: Int,
    var timeCreate: String? = null,
) {
    constructor(song: Song, timeCreate: String?) : this(
        id = 0,
        idSong = song.idSong,
        title = song.title,
        nameSinger = song.title,
        avatar = song.avatar,
        sing = song.sing,
        time = song.time,
        type = song.type,
        timeCreate = timeCreate
    )
}