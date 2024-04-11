package com.example.serviceandroid.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Int,
    val title: String,
    val nameSinger: String,
    val avatar: Int,
    val sing: Int,
    val time: Int,
    val type: Int
) : Parcelable {
    fun checkMusicNational(national: National): Boolean {
        return when (national) {
            National.VIETNAMESE -> type == 0
            National.INTERNATIONAL -> type == 1
            else -> true
        }
    }
}