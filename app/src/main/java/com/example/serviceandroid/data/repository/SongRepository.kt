package com.example.serviceandroid.data.repository

import com.example.serviceandroid.model.Song

/**
 * Single source for the app playlist (demo data; replace with remote/local later).
 */
interface SongRepository {
    fun getPlaylist(): ArrayList<Song>
    fun getSong(index: Int): Song
    fun indexOf(song: Song): Int
    fun lastIndex(): Int
    fun size(): Int
}
