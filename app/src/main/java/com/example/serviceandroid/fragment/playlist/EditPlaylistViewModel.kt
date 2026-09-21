package com.example.serviceandroid.fragment.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.playlist.PlaylistMutationResult
import com.example.serviceandroid.data.playlist.PlaylistRepository
import com.example.serviceandroid.data.playlist.UserPlaylist
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EditPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>(ARG_PLAYLIST_ID).orEmpty()

    val playlist: StateFlow<UserPlaylist?> = playlistRepository.observePlaylist(playlistId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val songs: StateFlow<List<Song>> = playlistRepository.observeSongs(playlistId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun savePlaylist(
        title: String,
        isPublic: Boolean,
        onResult: (PlaylistMutationResult) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(playlistRepository.updatePlaylist(playlistId, title, isPublic))
        }
    }

    fun persistOrder(songs: List<Song>, onResult: (PlaylistMutationResult) -> Unit = {}) {
        viewModelScope.launch {
            onResult(
                playlistRepository.reorderSongs(
                    playlistId = playlistId,
                    songIds = songs.map { it.id },
                    firstCoverUrl = songs.firstOrNull()?.thumbnailUrl.orEmpty(),
                ),
            )
        }
    }

    companion object {
        const val ARG_PLAYLIST_ID = "playlistId"
    }
}
