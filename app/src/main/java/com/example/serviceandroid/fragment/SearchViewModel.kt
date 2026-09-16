package com.example.serviceandroid.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.data.search.SearchCatalog
import com.example.serviceandroid.data.search.SearchHistoryRepository
import com.example.serviceandroid.data.search.SearchQuery
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainSinger
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
    val recentQueries: List<SearchQuery> = emptyList(),
    val suggestions: List<String> = emptyList(),
) {
    val hasResults: Boolean get() = songs.isNotEmpty() || singers.isNotEmpty()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val firestore: FirestoreMusicRepository,
    private val songRepository: SongRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestionPool: List<String> = emptyList()

    init {
        viewModelScope.launch {
            searchHistoryRepository.observeRecentQueries(SearchCatalog.HISTORY_LIMIT).collect { queries ->
                _uiState.value = _uiState.value.copy(
                    recentQueries = queries,
                    suggestions = SearchCatalog.pickSuggestions(suggestionPool, queries),
                )
            }
        }
        viewModelScope.launch {
            loadSuggestions()
        }
    }

    fun search(query: String) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            clearResults()
            return
        }
        val current = _uiState.value
        if (keyword == current.query && !current.isSearching) return

        viewModelScope.launch {
            _uiState.value = current.copy(query = keyword, isSearching = true)
            ensureCatalogLoaded()
            val songsDeferred = async {
                SearchCatalog.filterSongs(catalogSongs(), keyword)
            }
            val singersDeferred = async {
                runCatching {
                    SearchCatalog.filterSingers(
                        firestore.getSingers().map { it.toDomainSinger() },
                        keyword,
                    )
                }.getOrDefault(emptyList())
            }
            _uiState.value = _uiState.value.copy(
                query = keyword,
                songs = songsDeferred.await(),
                singers = singersDeferred.await(),
                isSearching = false,
            )
        }
    }

    fun recordCurrentQuery() {
        val keyword = _uiState.value.query.trim()
        if (keyword.isBlank()) return
        viewModelScope.launch {
            searchHistoryRepository.recordQuery(keyword)
        }
    }

    fun deleteRecentQuery(normalizedQuery: String) {
        viewModelScope.launch {
            searchHistoryRepository.deleteQuery(normalizedQuery)
        }
    }

    fun clearRecentQueries() {
        viewModelScope.launch {
            searchHistoryRepository.clearAll()
        }
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            query = "",
            songs = emptyList(),
            singers = emptyList(),
            isSearching = false,
        )
    }

    private suspend fun loadSuggestions() {
        if (songRepository.getTopPlaylist().isEmpty()) {
            songRepository.refreshTopPlaylist()
        }
        val titles = songRepository.getTopPlaylist().map { it.title }
        val categories = runCatching {
            firestore.getCategories().map { it.name }
        }.getOrDefault(emptyList())
        suggestionPool = titles + categories
        _uiState.value = _uiState.value.copy(
            suggestions = SearchCatalog.pickSuggestions(suggestionPool, _uiState.value.recentQueries),
        )
    }

    private suspend fun ensureCatalogLoaded() {
        if (songRepository.getLatestPlaylist().isEmpty()) {
            songRepository.refreshPlaylist()
        }
        if (songRepository.getTopPlaylist().isEmpty()) {
            songRepository.refreshTopPlaylist()
        }
    }

    private fun catalogSongs(): List<Song> =
        SearchCatalog.mergeSongs(
            latest = songRepository.getLatestPlaylist(),
            top = songRepository.getTopPlaylist(),
        )
}
