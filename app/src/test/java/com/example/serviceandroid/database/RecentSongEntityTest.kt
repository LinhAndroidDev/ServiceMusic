package com.example.serviceandroid.database

import com.example.serviceandroid.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSongEntityTest {

    @Test
    fun fromSongAndBack_preservesPlaybackMetadata() {
        val song = Song(
            id = "song-1",
            title = "Bài hát",
            nameSinger = "Ca sĩ",
            thumbnailUrl = "https://example.com/image.jpg",
            audioUrl = "https://example.com/audio.mp3",
            lyricUrl = "https://example.com/lyric.lrc",
            durationSec = 180,
            categoryId = "category-1",
            categoryName = "Nhạc Việt",
            views = 42,
        )

        val entity = RecentSongEntity.fromSong(
            song = song,
            lastPlayedAt = 1234L,
            playCount = 3L,
        )

        assertEquals(song, entity.toSong())
        assertEquals(1234L, entity.lastPlayedAt)
        assertEquals(3L, entity.playCount)
    }
}
