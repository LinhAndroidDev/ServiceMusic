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
    /** Replace the active playback queue (e.g. downloaded / favourites offline list). */
    fun setPlaybackQueue(songs: List<Song>)
    /**
     * Ensures [songId] is in the active playback queue.
     * If missing from the current queue but present in latest/top cache, restores that cache as queue.
     * @return index in the active queue, or -1 if not found in any known list.
     */
    fun ensureQueueForSongId(songId: String): Int
    fun getSong(index: Int): Song
    fun getSongById(id: String): Song?
    fun indexOf(song: Song): Int
    fun lastIndex(): Int
    fun size(): Int
    fun isLoaded(): Boolean
}
