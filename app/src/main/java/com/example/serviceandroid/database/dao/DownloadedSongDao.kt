package com.example.serviceandroid.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.serviceandroid.database.DownloadStatus
import com.example.serviceandroid.database.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadedSongEntity)

    @Query("SELECT * FROM downloaded_song WHERE songId = :songId LIMIT 1")
    suspend fun getById(songId: String): DownloadedSongEntity?

    @Query("SELECT * FROM downloaded_song WHERE songId = :songId LIMIT 1")
    fun observeById(songId: String): Flow<DownloadedSongEntity?>

    @Query("SELECT * FROM downloaded_song WHERE status = :status ORDER BY downloadedAt DESC")
    fun observeByStatus(status: DownloadStatus): Flow<List<DownloadedSongEntity>>

    @Query("SELECT COUNT(*) FROM downloaded_song WHERE status = :status")
    fun observeCountByStatus(status: DownloadStatus): Flow<Int>

    @Query(
        """
        UPDATE downloaded_song
        SET status = :status,
            localAudioPath = :localAudioPath,
            localLyricPath = :localLyricPath,
            downloadedAt = :downloadedAt
        WHERE songId = :songId
        """
    )
    suspend fun updateDownloadResult(
        songId: String,
        status: DownloadStatus,
        localAudioPath: String,
        localLyricPath: String,
        downloadedAt: Long,
    )

    @Query("UPDATE downloaded_song SET status = :status WHERE songId = :songId")
    suspend fun updateStatus(songId: String, status: DownloadStatus)

    @Query("DELETE FROM downloaded_song WHERE songId = :songId")
    suspend fun deleteById(songId: String)
}
