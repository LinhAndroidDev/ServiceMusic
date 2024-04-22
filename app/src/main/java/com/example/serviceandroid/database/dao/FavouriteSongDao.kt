package com.example.serviceandroid.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.serviceandroid.model.Song

@Dao
interface FavouriteSongDao {
    @Query("SELECT * FROM songEntity")
    suspend fun getAll(): MutableList<Song>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Query("DELETE FROM songEntity WHERE id = :id")
    suspend fun deleteSongById(id: Int)
}