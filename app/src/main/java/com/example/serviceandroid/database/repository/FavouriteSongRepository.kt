package com.example.serviceandroid.database.repository

import com.example.serviceandroid.database.SongEntity
import com.example.serviceandroid.database.dao.FavouriteSongDao
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.ExtensionFunctions.toSong
import javax.inject.Inject

class FavouriteSongRepository @Inject constructor(private val dao: FavouriteSongDao) {
    suspend fun getAll(): MutableList<Song>? {
        return dao.getAll()?.map {
            songEntity -> songEntity.toSong()
        }?.toMutableList()
    }

    suspend fun insertSong(song: Song, timeCreate: String) {
        dao.insertSong(SongEntity(song, timeCreate))
    }

    suspend fun deleteSongById(id: Int) = dao.deleteSongById(id)

    suspend fun checkSongById(id: Int): Boolean {
        return dao.checkSongById(id) != null
    }
}