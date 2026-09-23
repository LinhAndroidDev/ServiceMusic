package com.example.serviceandroid.fragment.singer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.artist.FollowMutationResult
import com.example.serviceandroid.data.artist.FollowedSingerRepository
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainSinger
import com.example.serviceandroid.model.toDomainSong
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SingerDetailUiState(
    val singer: Singer? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: Boolean = false,
    val isFollowed: Boolean = false,
)

@HiltViewModel
class SingerDetailViewModel @Inject constructor(
    private val firestore: FirestoreMusicRepository,
    private val followedSingerRepository: FollowedSingerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SingerDetailUiState(isLoading = true))
    val uiState: StateFlow<SingerDetailUiState> = _uiState.asStateFlow()
    private var followJob: Job? = null

    fun load(singerId: String) {
        followJob?.cancel()
        if (singerId.isBlank()) {
            _uiState.value = SingerDetailUiState(isLoading = false, error = true)
            return
        }
        followJob = viewModelScope.launch {
            followedSingerRepository.observeIsFollowed(singerId).collect { followed ->
                _uiState.value = _uiState.value.copy(isFollowed = followed)
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = false)
            val singerDeferred = async {
                runCatching { firestore.getSinger(singerId)?.toDomainSinger() }.getOrNull()
            }
            val songsDeferred = async {
                runCatching {
                    firestore.getSongsBySinger(singerId).map { it.toDomainSong() }
                }.getOrDefault(emptyList())
            }
            val singer = singerDeferred.await()
            val songs = songsDeferred.await()
            _uiState.value = _uiState.value.copy(
                singer = singer,
                songs = songs,
                isLoading = false,
                error = singer == null,
            )
        }
    }

    fun follow(singer: Singer, onResult: (FollowMutationResult) -> Unit) {
        viewModelScope.launch {
            onResult(followedSingerRepository.follow(singer))
        }
    }

    fun unfollow(singerId: String, onResult: (FollowMutationResult) -> Unit) {
        viewModelScope.launch {
            onResult(followedSingerRepository.unfollow(singerId))
        }
    }
}
