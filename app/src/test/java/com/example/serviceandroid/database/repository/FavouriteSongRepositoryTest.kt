package com.example.serviceandroid.database.repository

import com.example.serviceandroid.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavouriteSongRepositoryTest {

    @Test
    fun writePrecondition_prioritizesOfflineThenRequiresLogin() {
        assertEquals(
            FavouriteMutationResult.Offline,
            favouriteWritePrecondition(isOnline = false, userId = null),
        )
        assertEquals(
            FavouriteMutationResult.RequiresLogin,
            favouriteWritePrecondition(isOnline = true, userId = null),
        )
        assertNull(favouriteWritePrecondition(isOnline = true, userId = "uid-1"))
    }

    @Test
    fun documentPath_isScopedByUserId() {
        assertEquals(
            "users/uid-1/favouriteSongs/song-1",
            favouriteDocumentPath("uid-1", "song-1"),
        )
    }

    @Test
    fun sortRecords_supportsNewestAndVietnameseSongName() {
        val records = listOf(
            FavouriteSongRecord(song("2", "Em Của Ngày Hôm Qua"), createdAtMillis = 200),
            FavouriteSongRecord(song("1", "Âm Thầm Bên Em"), createdAtMillis = 100),
        )

        assertEquals(
            listOf("2", "1"),
            sortFavouriteRecords(records, ArrangeMusic.NEWEST).map(Song::id),
        )
        assertEquals(
            listOf("1", "2"),
            sortFavouriteRecords(records, ArrangeMusic.BY_NAME_SONG).map(Song::id),
        )
    }

    private fun song(id: String, title: String) = Song(
        id = id,
        title = title,
        nameSinger = "Ca sĩ",
        thumbnailUrl = "",
        audioUrl = "https://example.com/$id.mp3",
    )
}
