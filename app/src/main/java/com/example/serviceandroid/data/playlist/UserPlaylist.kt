package com.example.serviceandroid.data.playlist

data class UserPlaylist(
    val id: String,
    val title: String,
    val isPublic: Boolean,
    val songCount: Int,
    val coverUrl: String,
    val createdAt: Long,
)
