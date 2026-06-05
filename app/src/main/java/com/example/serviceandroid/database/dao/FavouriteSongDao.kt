package com.example.serviceandroid.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.serviceandroid.database.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Query("SELECT * FROM songEntity")
    suspend fun getAll(): List<SongEntity>?

    @Query("SELECT COUNT(*) FROM songEntity")
    fun observeFavouriteCount(): Flow<Int>

    @Query("SELECT * FROM songEntity")
    fun observeAllEntities(): Flow<List<SongEntity>>

    @Query("DELETE FROM songEntity WHERE id = :id")
    suspend fun deleteSongById(id: String)

    @Query("SELECT * FROM songEntity WHERE id = :id")
    suspend fun checkSongById(id: String): SongEntity?
}
