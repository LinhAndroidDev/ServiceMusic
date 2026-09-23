package com.example.serviceandroid.data.search

import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.VietnameseFold

object SearchCatalog {
    const val RESULT_LIMIT = 20
    const val SUGGESTION_LIMIT = 6
    const val HISTORY_LIMIT = 10
    const val RELATED_NAME_LIMIT = 3

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

    fun relatedNames(
        songs: List<Song>,
        singers: List<Singer>,
        query: String = "",
        limit: Int = RELATED_NAME_LIMIT,
    ): List<String> {
        val titles = uniqueFoldedNames(songs.asSequence().map { it.title })
        val artists = uniqueFoldedNames(
            singers.asSequence().map { it.name } +
                songs.asSequence().map { it.nameSinger },
        )
        val foldedQuery = VietnameseFold.fold(query)
        val titleQueue = prioritizeQueryMatches(titles, foldedQuery).toMutableList()
        val artistQueue = prioritizeQueryMatches(artists, foldedQuery).toMutableList()

        val seen = mutableSetOf<String>()
        val names = ArrayList<String>(limit)
        var takeTitle = titleQueue.any { VietnameseFold.contains(it, foldedQuery) } ||
            artistQueue.none { VietnameseFold.contains(it, foldedQuery) }

        while (names.size < limit && (titleQueue.isNotEmpty() || artistQueue.isNotEmpty())) {
            val primary = if (takeTitle) titleQueue else artistQueue
            val fallback = if (takeTitle) artistQueue else titleQueue
            val next = primary.removeFirstOrNull() ?: fallback.removeFirstOrNull() ?: break
            val folded = VietnameseFold.fold(next)
            if (folded.isNotBlank() && seen.add(folded)) {
                names.add(next)
            }
            takeTitle = !takeTitle
        }
        return names
    }

    private fun uniqueFoldedNames(values: Sequence<String>): List<String> {
        val seen = mutableSetOf<String>()
        return values
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { candidate ->
                val folded = VietnameseFold.fold(candidate)
                folded.isNotBlank() && seen.add(folded)
            }
            .toList()
    }

    private fun prioritizeQueryMatches(names: List<String>, foldedQuery: String): List<String> {
        if (foldedQuery.isBlank()) return names
        val matches = names.filter { VietnameseFold.contains(it, foldedQuery) }
        val rest = names.filter { !VietnameseFold.contains(it, foldedQuery) }
        return matches + rest
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
