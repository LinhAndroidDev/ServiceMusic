package com.example.serviceandroid.fragment.singer

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

data class SingerDetailUiState(
    val singer: Singer? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class SingerDetailViewModel @Inject constructor(
    private val firestore: FirestoreMusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SingerDetailUiState(isLoading = true))
    val uiState: StateFlow<SingerDetailUiState> = _uiState.asStateFlow()

    fun load(singerId: String) {
        if (singerId.isBlank()) {
            _uiState.value = SingerDetailUiState(isLoading = false, error = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = SingerDetailUiState(isLoading = true)
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
            _uiState.value = SingerDetailUiState(
                singer = singer,
                songs = songs,
                isLoading = false,
                error = singer == null,
            )
        }
    }
}
