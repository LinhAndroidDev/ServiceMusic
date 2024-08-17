package com.example.serviceandroid.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.serviceandroid.database.SongEntity

@Dao
interface FavouriteSongDao {
    @Query("SELECT * FROM songEntity")
    suspend fun getAll(): List<SongEntity>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(songEntity: SongEntity)

    @Query("DELETE FROM songEntity WHERE idSong = :id")
    suspend fun deleteSongById(id: Int)

    @Query("SELECT * FROM songEntity WHERE idSong = :id")
    suspend fun checkSongById(id: Int): SongEntity?
}