package com.example.serviceandroid.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainSinger
import com.example.serviceandroid.model.toDomainSong
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val singers: List<Singer> = emptyList(),
    val isSearching: Boolean = false,
) {
    val hasResults: Boolean get() = songs.isNotEmpty() || singers.isNotEmpty()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val firestore: FirestoreMusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            clearResults()
            return
        }
        val current = _uiState.value
        // Avoid reloading when navigating back to search with the same query.
        if (keyword == current.query && !current.isSearching) return

        viewModelScope.launch {
            _uiState.value = current.copy(query = keyword, isSearching = true)
            val songsDeferred = async {
                runCatching {
                    firestore.searchSongsByTitle(keyword).map { it.toDomainSong() }
                }.getOrDefault(emptyList())
            }
            val singersDeferred = async {
                runCatching {
                    firestore.searchSingersByName(keyword).map { it.toDomainSinger() }
                }.getOrDefault(emptyList())
            }
            _uiState.value = SearchUiState(
                query = keyword,
                songs = songsDeferred.await(),
                singers = singersDeferred.await(),
                isSearching = false,
            )
        }
    }

    fun clearResults() {
        _uiState.value = SearchUiState()
    }
}
