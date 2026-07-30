package com.example.serviceandroid.data.repository

import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val firestore: FirestoreMusicRepository,
) : SongRepository {

    private val latestCache = mutableListOf<Song>()
    private val topCache = mutableListOf<Song>()
    /** Playback queue — last refreshed source (latest or top). */
    private val playbackQueue = mutableListOf<Song>()

    override suspend fun refreshPlaylist(): Result<Unit> = runCatching {
        val songs = firestore.getLatestSongs(limit = 100, fromServer = true)
            .map { it.toDomainSong() }
        synchronized(this) {
            if (songs.isEmpty() && latestCache.isNotEmpty()) return@runCatching
            latestCache.clear()
            latestCache.addAll(songs)
            playbackQueue.clear()
            playbackQueue.addAll(songs)
        }
    }

    override suspend fun refreshTopPlaylist(): Result<Unit> = runCatching {
        val songs = firestore.getTopSongs(limit = 100, fromServer = true)
            .map { it.toDomainSong() }
        synchronized(this) {
            if (songs.isEmpty() && topCache.isNotEmpty()) return@runCatching
            topCache.clear()
            topCache.addAll(songs)
            playbackQueue.clear()
            playbackQueue.addAll(songs)
        }
    }

    override fun getPlaylist(): List<Song> = synchronized(this) { playbackQueue.toList() }

    override fun getLatestPlaylist(): List<Song> = synchronized(this) { latestCache.toList() }

    override fun getTopPlaylist(): List<Song> = synchronized(this) { topCache.toList() }

    override fun setPlaybackQueue(songs: List<Song>) {
        synchronized(this) {
            playbackQueue.clear()
            playbackQueue.addAll(songs)
        }
    }

    override fun ensureQueueForSongId(songId: String): Int {
        if (songId.isBlank()) return -1
        synchronized(this) {
            val inQueue = playbackQueue.indexOfFirst { it.id == songId }
            if (inQueue >= 0) return inQueue

            val inLatest = latestCache.indexOfFirst { it.id == songId }
            if (inLatest >= 0) {
                playbackQueue.clear()
                playbackQueue.addAll(latestCache)
                return inLatest
            }

            val inTop = topCache.indexOfFirst { it.id == songId }
            if (inTop >= 0) {
                playbackQueue.clear()
                playbackQueue.addAll(topCache)
                return inTop
            }
            return -1
        }
    }

    override fun getSong(index: Int): Song {
        val list = getPlaylist()
        if (list.isEmpty()) throw IllegalStateException("Playlist not loaded")
        return list[index.coerceIn(0, list.lastIndex)]
    }

    override fun getSongById(id: String): Song? =
        synchronized(this) {
            playbackQueue.find { it.id == id }
                ?: latestCache.find { it.id == id }
                ?: topCache.find { it.id == id }
        }

    override fun indexOf(song: Song): Int =
        getPlaylist().indexOfFirst { it.id == song.id }

    override fun lastIndex(): Int {
        val list = getPlaylist()
        return if (list.isEmpty()) -1 else list.lastIndex
    }

    override fun size(): Int = getPlaylist().size

    override fun isLoaded(): Boolean = getPlaylist().isNotEmpty()
}
