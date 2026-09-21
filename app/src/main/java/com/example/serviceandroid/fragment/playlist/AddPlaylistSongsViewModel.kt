package com.example.serviceandroid.fragment.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.playlist.PlaylistMutationResult
import com.example.serviceandroid.data.playlist.PlaylistRepository
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.data.search.SearchCatalog
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddPlaylistSongsUiState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val addedSongIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AddPlaylistSongsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>(ARG_PLAYLIST_ID).orEmpty()
    private val addedIds = mutableSetOf<String>()
    private var randomSuggestions: List<Song> = emptyList()
    private var currentCoverUrl: String = ""
    private var playlistTitle: String = ""
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(AddPlaylistSongsUiState())
    val uiState: StateFlow<AddPlaylistSongsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playlistRepository.observePlaylist(playlistId).collect { playlist ->
                currentCoverUrl = playlist?.coverUrl.orEmpty()
                playlistTitle = playlist?.title.orEmpty()
            }
        }
        viewModelScope.launch {
            playlistRepository.observeSongs(playlistId).collect { songs ->
                addedIds.clear()
                addedIds.addAll(songs.map { it.id })
                if (_uiState.value.query.isBlank()) {
                    publishSuggestions()
                } else {
                    _uiState.value = _uiState.value.copy(addedSongIds = addedIds.toSet())
                }
            }
        }
        viewModelScope.launch {
            loadSuggestions()
        }
    }

    fun onQueryChanged(raw: String) {
        searchJob?.cancel()
        val query = raw.trim()
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            publishSuggestions()
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            ensureCatalogLoaded()
            _uiState.value = _uiState.value.copy(
                songs = SearchCatalog.filterSongs(catalogSongs(), query),
                isLoading = false,
                addedSongIds = addedIds.toSet(),
            )
        }
    }

    fun addSong(song: Song, onResult: (PlaylistMutationResult) -> Unit) {
        if (song.id in addedIds) {
            onResult(PlaylistMutationResult.AlreadyExists)
            return
        }
        viewModelScope.launch {
            val result = playlistRepository.addSong(playlistId, song, currentCoverUrl)
            if (result is PlaylistMutationResult.Success) {
                addedIds.add(song.id)
                currentCoverUrl = currentCoverUrl.ifBlank { song.thumbnailUrl }
                if (_uiState.value.query.isBlank()) {
                    publishSuggestions()
                } else {
                    _uiState.value = _uiState.value.copy(addedSongIds = addedIds.toSet())
                }
            }
            onResult(result)
        }
    }

    fun currentPlaylistTitle(): String = playlistTitle

    private suspend fun loadSuggestions() {
        ensureCatalogLoaded()
        randomSuggestions = songRepository.getTopPlaylist()
            .filter { it.id !in addedIds }
            .shuffled()
            .take(SUGGESTION_LIMIT)
        publishSuggestions()
    }

    private fun publishSuggestions() {
        if (randomSuggestions.isEmpty() && songRepository.getTopPlaylist().isNotEmpty()) {
            randomSuggestions = songRepository.getTopPlaylist()
                .filter { it.id !in addedIds }
                .shuffled()
                .take(SUGGESTION_LIMIT)
        }
        _uiState.value = _uiState.value.copy(
            songs = randomSuggestions.filter { it.id !in addedIds },
            isLoading = false,
            addedSongIds = addedIds.toSet(),
        )
    }

    private suspend fun ensureCatalogLoaded() {
        if (songRepository.getTopPlaylist().isEmpty()) {
            songRepository.refreshTopPlaylist()
        }
        if (songRepository.getLatestPlaylist().isEmpty()) {
            songRepository.refreshPlaylist()
        }
    }

    private fun catalogSongs(): List<Song> =
        SearchCatalog.mergeSongs(
            latest = songRepository.getLatestPlaylist(),
            top = songRepository.getTopPlaylist(),
        )

    companion object {
        const val ARG_PLAYLIST_ID = "playlistId"
        const val SUGGESTION_LIMIT = 20
    }
}
