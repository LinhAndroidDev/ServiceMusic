package com.example.serviceandroid.database.repository

import com.example.serviceandroid.database.dao.FavouriteSongDao
import com.example.serviceandroid.model.Song
import javax.inject.Inject

class FavouriteSongRepository @Inject constructor(private val dao: FavouriteSongDao) {
    suspend fun getAll(): MutableList<Song>? = dao.getAll()

    suspend fun insertSong(song: Song) = dao.insertSong(song)

    suspend fun deleteSongById(id: Int) = dao.deleteSongById(id)
}