package com.example.serviceandroid.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.serviceandroid.database.dao.DownloadedSongDao
import com.example.serviceandroid.database.dao.RecentSongDao

class DownloadStatusConverters {
    @TypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name
}

@Database(
    entities = [DownloadedSongEntity::class, RecentSongEntity::class],
    version = MusicDatabase.VERSION,
    exportSchema = false,
)
@TypeConverters(DownloadStatusConverters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun recentSongDao(): RecentSongDao

    companion object {
        const val VERSION = 5
    }
}
