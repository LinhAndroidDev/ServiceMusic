package com.example.serviceandroid.fragment.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.playlist.PlaylistMutationResult
import com.example.serviceandroid.data.playlist.PlaylistRepository
import com.example.serviceandroid.data.playlist.UserPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<UserPlaylist>> = playlistRepository.observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun createPlaylist(
        title: String,
        isPublic: Boolean,
        onResult: (PlaylistMutationResult) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(playlistRepository.createPlaylist(title, isPublic))
        }
    }
}
