package com.example.serviceandroid.model

enum class ListenRecentType {
    RECENT, PLAYLIST, MV
}
data class ListenRecent(
    val name: String,
    val image: Int,
    val type: ListenRecentType
)