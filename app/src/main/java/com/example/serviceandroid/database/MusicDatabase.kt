package com.example.serviceandroid.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.serviceandroid.database.dao.DownloadedSongDao
import com.example.serviceandroid.database.dao.FavouriteSongDao

class DownloadStatusConverters {
    @TypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name
}

@Database(
    entities = [SongEntity::class, DownloadedSongEntity::class],
    version = MusicDatabase.VERSION,
    exportSchema = false,
)
@TypeConverters(DownloadStatusConverters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun favouriteSongDao(): FavouriteSongDao
    abstract fun downloadedSongDao(): DownloadedSongDao

    companion object {
        const val VERSION = 2
    }
}
