package com.example.serviceandroid.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.serviceandroid.database.dao.FavouriteSongDao
import com.example.serviceandroid.model.Song

@Database(entities = [Song::class], version = 1)

abstract class MusicDatabase : RoomDatabase() {
    abstract fun favouriteSongDao(): FavouriteSongDao
}