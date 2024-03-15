package com.example.serviceandroid.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val title: String,
    val nameSinger: String,
    val avatar: Int,
    val sing: Int,
    val time: Int,
    val type: Int
): Parcelable