package com.example.serviceandroid.fragment.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.recent.RecentHistoryRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RecentHistoryViewModel @Inject constructor(
    recentHistoryRepository: RecentHistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<RecentHistoryUiState> = recentHistoryRepository
        .observeRecentSongs(limit = FULL_HISTORY_LIMIT)
        .map { songs ->
            RecentHistoryUiState(
                songs = songs,
                previewSongs = songs.take(PREVIEW_LIMIT),
                showSeeAll = songs.size > PREVIEW_LIMIT,
                isLoading = false,
            )
        }
        .catch {
            emit(
                RecentHistoryUiState(
                    isLoading = false,
                    errorMessage = "Không thể tải lịch sử nghe",
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecentHistoryUiState(isLoading = true),
        )

    companion object {
        const val PREVIEW_LIMIT = 5
        const val FULL_HISTORY_LIMIT = 100
    }
}

data class RecentHistoryUiState(
    val songs: List<Song> = emptyList(),
    val previewSongs: List<Song> = emptyList(),
    val showSeeAll: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
