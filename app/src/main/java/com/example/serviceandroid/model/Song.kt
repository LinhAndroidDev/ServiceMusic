package com.example.serviceandroid.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "songEntity")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val nameSinger: String,
    val avatar: Int,
    val sing: Int,
    val time: Int,
    val type: Int,
    var timeCreate: String? = null,
) : Parcelable {
    fun checkMusicNational(national: National): Boolean {
        return when (national) {
            National.VIETNAMESE -> type == 0
            National.INTERNATIONAL -> type == 1
            else -> true
        }
    }

    fun getSongById(idSong: Int): Boolean {
        return idSong == id
    }
}