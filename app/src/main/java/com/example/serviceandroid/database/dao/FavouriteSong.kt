package com.example.serviceandroid.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface FavouriteSong {
    @Query("SELECT * FROM favouriteSongEntity")
    fun getAll(): List<FavouriteSong>
}