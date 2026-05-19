package com.example.serviceandroid.data.repository

import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor() : SongRepository {

    override fun getPlaylist(): ArrayList<Song> = Data.listMusic()

    override fun getSong(index: Int): Song = getPlaylist()[index.coerceIn(0, lastIndex())]

    override fun indexOf(song: Song): Int = getPlaylist().indexOfFirst { it.idSong == song.idSong }

    override fun lastIndex(): Int = (getPlaylist().size - 1).coerceAtLeast(0)

    override fun size(): Int = getPlaylist().size
}
