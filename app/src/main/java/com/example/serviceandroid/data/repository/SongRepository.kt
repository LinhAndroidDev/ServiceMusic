package com.example.serviceandroid.data.repository

import com.example.serviceandroid.model.Song

/**
 * Cached playlists from Firestore. [getPlaylist] is the active playback queue;
 * [getLatestPlaylist] / [getTopPlaylist] retain per-screen caches across tab switches.
 */
interface SongRepository {
    suspend fun refreshPlaylist(): Result<Unit>
    suspend fun refreshTopPlaylist(): Result<Unit>
    /** Active queue used by [MusicService] / playback. */
    fun getPlaylist(): List<Song>
    fun getLatestPlaylist(): List<Song>
    fun getTopPlaylist(): List<Song>
    fun getSong(index: Int): Song
    fun getSongById(id: String): Song?
    fun indexOf(song: Song): Int
    fun lastIndex(): Int
    fun size(): Int
    fun isLoaded(): Boolean
}
