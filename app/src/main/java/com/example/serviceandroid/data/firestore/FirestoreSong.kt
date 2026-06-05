package com.example.serviceandroid.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirestoreSong(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val singerIds: List<String> = emptyList(),
    val singerNames: List<String> = emptyList(),
    val singerId: String = "",
    val singerName: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val thumbnailUrl: String = "",
    val audioUrl: String = "",
    val lyricUrl: String = "",
    val duration: Long = 0,
    val views: Long = 0,
    val createdAt: Timestamp? = null,
) {
    val displaySingerIds: List<String>
        get() = if (singerIds.isNotEmpty()) singerIds
        else if (singerId.isNotEmpty()) listOf(singerId)
        else emptyList()

    val displaySingerNames: List<String>
        get() = if (singerNames.isNotEmpty()) singerNames
        else if (singerName.isNotEmpty()) listOf(singerName)
        else emptyList()

    val artistText: String
        get() = displaySingerNames.joinToString(", ")

    val hasLyric: Boolean
        get() = lyricUrl.isNotBlank()
}
