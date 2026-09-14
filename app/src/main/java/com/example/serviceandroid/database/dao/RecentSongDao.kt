package com.example.serviceandroid.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.serviceandroid.database.RecentSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentSongEntity)

    @Query("SELECT * FROM recent_song WHERE songId = :songId LIMIT 1")
    suspend fun getById(songId: String): RecentSongEntity?

    @Query("SELECT * FROM recent_song ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<RecentSongEntity>>

    @Query("SELECT * FROM recent_song ORDER BY lastPlayedAt DESC")
    suspend fun getAll(): List<RecentSongEntity>

    @Query("SELECT COUNT(*) FROM recent_song")
    suspend fun count(): Int

    @Query("DELETE FROM recent_song")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM recent_song
        WHERE songId NOT IN (
            SELECT songId FROM recent_song
            ORDER BY lastPlayedAt DESC
            LIMIT :maxItems
        )
        """
    )
    suspend fun trimTo(maxItems: Int)
}
