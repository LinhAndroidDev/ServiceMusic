package com.example.serviceandroid.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favouriteSongEntity")
data class FavouriteSongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val nameSinger: String,
    val avatar: Int,
    val sing: Int,
    val time: Int,
    val type: Int,
    val timeCreate: String,
)