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

    private val playlist = mutableListOf<Song>()

    override suspend fun refreshPlaylist(): Result<Unit> = runCatching {
        val songs = firestore.getLatestSongs(limit = 100)
            .map { it.toDomainSong() }
        synchronized(playlist) {
            playlist.clear()
            playlist.addAll(songs)
        }
    }

    override suspend fun refreshTopPlaylist(): Result<Unit> = runCatching {
        val songs = firestore.getTopSongs(limit = 100)
            .map { it.toDomainSong() }
        synchronized(playlist) {
            playlist.clear()
            playlist.addAll(songs)
        }
    }

    override fun getPlaylist(): List<Song> = synchronized(playlist) { playlist.toList() }

    override fun getSong(index: Int): Song {
        val list = getPlaylist()
        if (list.isEmpty()) throw IllegalStateException("Playlist not loaded")
        return list[index.coerceIn(0, list.lastIndex)]
    }

    override fun getSongById(id: String): Song? =
        getPlaylist().find { it.id == id }

    override fun indexOf(song: Song): Int =
        getPlaylist().indexOfFirst { it.id == song.id }

    override fun lastIndex(): Int {
        val list = getPlaylist()
        return if (list.isEmpty()) -1 else list.lastIndex
    }

    override fun size(): Int = getPlaylist().size

    override fun isLoaded(): Boolean = getPlaylist().isNotEmpty()
}
