package com.example.serviceandroid.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.serviceandroid.database.MusicDatabase
import com.example.serviceandroid.database.dao.FavouriteSongDao
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
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database.db"
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFavouriteSong(musicDatabase: MusicDatabase): FavouriteSongDao {
        return musicDatabase.favouriteSongDao()
    }

    @Provides
    fun provideSharePreference(@ApplicationContext context: Context): SharePreferenceRepository =
        SharePreferenceRepositoryImpl(context)
}