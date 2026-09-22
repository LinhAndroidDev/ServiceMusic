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

enum class SearchResultsMode {
    TYPING,
    COMMITTED,
}

data class SearchUiState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val singers: List<Singer> = emptyList(),
    val relatedNames: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val mode: SearchResultsMode = SearchResultsMode.TYPING,
    val recentQueries: List<SearchQuery> = emptyList(),
    val suggestions: List<String> = emptyList(),
) {
    val hasResults: Boolean get() = songs.isNotEmpty() || singers.isNotEmpty() || relatedNames.isNotEmpty()
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
    private var suggestionCategories: List<String> = emptyList()

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
        runSearch(query, SearchResultsMode.TYPING)
    }

    fun commitQuery(raw: String) {
        val keyword = raw.trim()
        if (keyword.isNotBlank()) {
            viewModelScope.launch {
                searchHistoryRepository.recordQuery(keyword)
            }
        }
        runSearch(raw, SearchResultsMode.COMMITTED)
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
            relatedNames = emptyList(),
            isSearching = false,
            mode = SearchResultsMode.TYPING,
        )
    }

    private fun runSearch(raw: String, mode: SearchResultsMode) {
        val keyword = raw.trim()
        if (keyword.isEmpty()) {
            clearResults()
            return
        }
        val current = _uiState.value
        if (keyword == current.query && !current.isSearching && current.mode == mode) return

        viewModelScope.launch {
            _uiState.value = current.copy(query = keyword, isSearching = true, mode = mode)
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
            val songs = songsDeferred.await()
            val singers = singersDeferred.await()
            _uiState.value = _uiState.value.copy(
                query = keyword,
                songs = songs,
                singers = singers,
                relatedNames = SearchCatalog.relatedNames(songs, singers, keyword),
                isSearching = false,
                mode = mode,
            )
        }
    }

    private suspend fun loadSuggestions() {
        publishSuggestions()
        if (songRepository.getTopPlaylist().isEmpty()) {
            songRepository.refreshTopPlaylist(fromServer = false)
            publishSuggestions()
        }
        val categories = runCatching {
            firestore.getCategories().map { it.name }
        }.getOrDefault(emptyList())
        if (categories.isEmpty()) return
        suggestionCategories = categories
        publishSuggestions()
    }

    private fun publishSuggestions() {
        val titles = songRepository.getTopPlaylist().map { it.title }
        suggestionPool = titles + suggestionCategories
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
