package com.example.serviceandroid.model

import android.os.Parcelable
import com.example.serviceandroid.data.firestore.FirestoreSong
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: String,
    val title: String,
    val nameSinger: String,
    val thumbnailUrl: String,
    val audioUrl: String,
    val lyricUrl: String = "",
    val durationSec: Long = 0,
    val categoryId: String = "",
    val categoryName: String = "",
    val views: Long = 0,
) : Parcelable {

    fun checkMusicNational(national: National): Boolean {
        return when (national) {
            National.VIETNAMESE -> categoryName.contains("Việt", ignoreCase = true) ||
                categoryName.contains("Viet", ignoreCase = true)
            National.INTERNATIONAL -> !categoryName.contains("Việt", ignoreCase = true) &&
                !categoryName.contains("Viet", ignoreCase = true) &&
                categoryName.isNotBlank()
            else -> true
        }
    }
}

fun FirestoreSong.toDomainSong(): Song = Song(
    id = id,
    title = title,
    nameSinger = artistText.ifBlank { singerName },
    thumbnailUrl = thumbnailUrl,
    audioUrl = audioUrl,
    lyricUrl = lyricUrl,
    durationSec = duration,
    categoryId = categoryId,
    categoryName = categoryName,
    views = views,
)
