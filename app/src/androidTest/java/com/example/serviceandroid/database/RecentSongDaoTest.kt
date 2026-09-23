package com.example.serviceandroid.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentSongDaoTest {
    private lateinit var database: MusicDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertAndObserveLatest_ordersAndLimitsHistory() = runBlocking {
        val dao = database.recentSongDao()
        dao.upsert(recentSong("a", lastPlayedAt = 100))
        dao.upsert(recentSong("b", lastPlayedAt = 200))
        dao.upsert(recentSong("c", lastPlayedAt = 150))

        assertEquals(listOf("b", "c"), dao.observeLatest(2).first().map { it.songId })

        dao.upsert(recentSong("a", lastPlayedAt = 300, playCount = 2))

        assertEquals(listOf("a", "b"), dao.observeLatest(2).first().map { it.songId })
        assertEquals(2L, dao.getById("a")?.playCount)
    }

    private fun recentSong(
        id: String,
        lastPlayedAt: Long,
        playCount: Long = 1,
    ) = RecentSongEntity(
        songId = id,
        title = "Song $id",
        nameSinger = "Singer",
        thumbnailUrl = "",
        audioUrl = "https://example.com/$id.mp3",
        lyricUrl = "",
        durationSec = 120,
        categoryId = "",
        categoryName = "",
        views = 0,
        lastPlayedAt = lastPlayedAt,
        playCount = playCount,
    )
}
