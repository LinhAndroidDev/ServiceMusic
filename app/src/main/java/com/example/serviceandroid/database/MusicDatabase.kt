package com.example.serviceandroid.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.serviceandroid.database.dao.FavouriteSongDao

@Database(
    entities = [SongEntity::class],
    version = MusicDatabase.VERSION,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun favouriteSongDao(): FavouriteSongDao

    companion object {
        const val VERSION = 3
    }
}
