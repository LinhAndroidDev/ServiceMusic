package com.example.serviceandroid.data.search

import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCatalogTest {

    @Test
    fun filterSongs_matchesFoldedTitleArtistAndCategory() {
        val songs = listOf(
            song("1", "Chúng Ta Của Hiện Tại", "Sơn Tùng M-TP", "Nhạc Việt"),
            song("2", "Đom Đóm", "Jack", "Nhạc Việt"),
            song("3", "Shape of You", "Ed Sheeran", "Pop"),
        )

        assertEquals(listOf("1"), SearchCatalog.filterSongs(songs, "son tung").map(Song::id))
        assertEquals(listOf("2"), SearchCatalog.filterSongs(songs, "dom dom").map(Song::id))
        assertEquals(listOf("3"), SearchCatalog.filterSongs(songs, "pop").map(Song::id))
    }

    @Test
    fun filterSingers_matchesUnsignedName() {
        val singers = listOf(
            Singer("a", "Sơn Tùng M-TP", "", ""),
            Singer("b", "Đen Vâu", "", ""),
        )

        assertEquals(listOf("a"), SearchCatalog.filterSingers(singers, "son tung").map(Singer::id))
        assertEquals(listOf("b"), SearchCatalog.filterSingers(singers, "den vau").map(Singer::id))
    }

    @Test
    fun mergeSongs_unionsLatestAndTopById() {
        val latest = listOf(song("1", "Mới", "A", "Việt"), song("2", "Cũ hơn", "B", "Việt"))
        val top = listOf(song("2", "Cũ hơn", "B", "Việt"), song("3", "Top", "C", "Việt"))

        assertEquals(listOf("1", "2", "3"), SearchCatalog.mergeSongs(latest, top).map(Song::id))
    }

    @Test
    fun pickSuggestions_skipsRecentFoldedDuplicates() {
        val pool = listOf("Sơn Tùng", "Nhạc Việt", "Workout", "son tung")
        val recent = listOf(SearchQuery("Sơn Tùng", "son tung"))

        assertEquals(
            listOf("Nhạc Việt", "Workout"),
            SearchCatalog.pickSuggestions(pool, recent),
        )
    }

    @Test
    fun searchQueryDocumentId_sanitizesPathCharacters() {
        assertEquals("son tung", searchQueryDocumentId("son tung"))
        assertEquals("a_b", searchQueryDocumentId("a/b"))
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        category: String,
    ) = Song(
        id = id,
        title = title,
        nameSinger = artist,
        thumbnailUrl = "",
        audioUrl = "",
        categoryName = category,
    )
}
