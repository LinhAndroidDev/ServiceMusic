package com.example.serviceandroid.di

import com.example.serviceandroid.data.playlist.PlaylistRepository
import com.example.serviceandroid.data.playlist.PlaylistRepositoryImpl
import com.example.serviceandroid.data.recent.RecentHistoryRepository
import com.example.serviceandroid.data.recent.RecentHistoryRepositoryImpl
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.data.repository.SongRepositoryImpl
import com.example.serviceandroid.data.search.SearchHistoryRepository
import com.example.serviceandroid.data.search.SearchHistoryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds
    @Singleton
    abstract fun bindRecentHistoryRepository(
        impl: RecentHistoryRepositoryImpl,
    ): RecentHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        impl: SearchHistoryRepositoryImpl,
    ): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl,
    ): PlaylistRepository
}
