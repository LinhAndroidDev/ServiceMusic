package com.example.serviceandroid.data.repository

import com.example.serviceandroid.model.Song

/**
 * Cached playlist from Firestore (refreshed via [refreshPlaylist]).
 */
interface SongRepository {
    suspend fun refreshPlaylist(): Result<Unit>
    suspend fun refreshTopPlaylist(): Result<Unit>
    fun getPlaylist(): List<Song>
    fun getSong(index: Int): Song
    fun getSongById(id: String): Song?
    fun indexOf(song: Song): Int
    fun lastIndex(): Int
    fun size(): Int
    fun isLoaded(): Boolean
}
