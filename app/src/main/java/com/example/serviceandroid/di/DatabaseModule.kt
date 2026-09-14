package com.example.serviceandroid.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.serviceandroid.database.MusicDatabase
import com.example.serviceandroid.database.dao.DownloadedSongDao
import com.example.serviceandroid.database.dao.FavouriteSongDao
import com.example.serviceandroid.database.dao.RecentSongDao
import com.example.serviceandroid.utils.SharePreferenceRepository
import com.example.serviceandroid.utils.SharePreferenceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recent_song` (
                    `songId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `nameSinger` TEXT NOT NULL,
                    `thumbnailUrl` TEXT NOT NULL,
                    `audioUrl` TEXT NOT NULL,
                    `lyricUrl` TEXT NOT NULL,
                    `durationSec` INTEGER NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `categoryName` TEXT NOT NULL,
                    `views` INTEGER NOT NULL,
                    `lastPlayedAt` INTEGER NOT NULL,
                    `playCount` INTEGER NOT NULL,
                    PRIMARY KEY(`songId`)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database.db"
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigrationFrom(1, 2)
            .build()
    }

    @Provides
    fun provideFavouriteSong(musicDatabase: MusicDatabase): FavouriteSongDao {
        return musicDatabase.favouriteSongDao()
    }

    @Provides
    fun provideDownloadedSongDao(musicDatabase: MusicDatabase): DownloadedSongDao {
        return musicDatabase.downloadedSongDao()
    }

    @Provides
    fun provideRecentSongDao(musicDatabase: MusicDatabase): RecentSongDao {
        return musicDatabase.recentSongDao()
    }

    @Provides
    fun provideSharePreference(@ApplicationContext context: Context): SharePreferenceRepository =
        SharePreferenceRepositoryImpl(context)
}