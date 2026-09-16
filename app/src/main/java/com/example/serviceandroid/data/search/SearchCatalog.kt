package com.example.serviceandroid.data.search

import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.VietnameseFold

object SearchCatalog {
    const val RESULT_LIMIT = 20
    const val SUGGESTION_LIMIT = 6
    const val HISTORY_LIMIT = 10

    fun mergeSongs(latest: List<Song>, top: List<Song>): List<Song> {
        val byId = LinkedHashMap<String, Song>()
        latest.forEach { song ->
            if (song.id.isNotBlank()) byId[song.id] = song
        }
        top.forEach { song ->
            if (song.id.isNotBlank()) byId.putIfAbsent(song.id, song)
        }
        return byId.values.toList()
    }

    fun filterSongs(songs: List<Song>, query: String): List<Song> {
        val folded = VietnameseFold.fold(query)
        if (folded.isBlank()) return emptyList()
        return songs.asSequence()
            .filter { song ->
                VietnameseFold.contains(song.title, folded) ||
                    VietnameseFold.contains(song.nameSinger, folded) ||
                    VietnameseFold.contains(song.categoryName, folded)
            }
            .take(RESULT_LIMIT)
            .toList()
    }

    fun filterSingers(singers: List<Singer>, query: String): List<Singer> {
        val folded = VietnameseFold.fold(query)
        if (folded.isBlank()) return emptyList()
        return singers.asSequence()
            .filter { VietnameseFold.contains(it.name, folded) }
            .take(RESULT_LIMIT)
            .toList()
    }

    fun pickSuggestions(
        pool: List<String>,
        recentQueries: List<SearchQuery>,
        limit: Int = SUGGESTION_LIMIT,
    ): List<String> {
        val recentFolded = recentQueries.map { it.normalizedQuery }.toSet()
        val seen = mutableSetOf<String>()
        return pool.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { candidate ->
                val folded = VietnameseFold.fold(candidate)
                folded.isNotBlank() && folded !in recentFolded && seen.add(folded)
            }
            .take(limit)
            .toList()
    }
}
