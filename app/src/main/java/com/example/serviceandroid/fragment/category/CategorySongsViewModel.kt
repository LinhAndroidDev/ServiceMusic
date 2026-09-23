package com.example.serviceandroid.fragment.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainSong
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CategorySongsMode {
    LATEST,
    TOP,
    CATEGORY,
}

@HiltViewModel
class CategorySongsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestoreMusicRepository: FirestoreMusicRepository,
) : ViewModel() {

    val title: String = savedStateHandle.get<String>(ARG_TITLE).orEmpty()
    private val mode = savedStateHandle.get<String>(ARG_MODE)
        ?.let { runCatching { CategorySongsMode.valueOf(it) }.getOrNull() }
        ?: CategorySongsMode.CATEGORY
    private val categoryId: String = savedStateHandle.get<String>(ARG_CATEGORY_ID).orEmpty()

    private val _uiState = MutableStateFlow(CategorySongsUiState(isLoading = true))
    val uiState: StateFlow<CategorySongsUiState> = _uiState.asStateFlow()

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val songs = runCatching { fetchSongs() }.getOrDefault(emptyList())
            _uiState.value = CategorySongsUiState(
                songs = songs,
                isLoading = false,
            )
        }
    }

    private suspend fun fetchSongs(): List<Song> {
        val documents = when (mode) {
            CategorySongsMode.LATEST -> firestoreMusicRepository.getLatestSongs()
            CategorySongsMode.TOP -> firestoreMusicRepository.getTopSongs()
            CategorySongsMode.CATEGORY -> {
                if (categoryId.isBlank()) emptyList()
                else firestoreMusicRepository.getSongsByCategory(categoryId)
            }
        }
        return documents.map { it.toDomainSong() }
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MODE = "mode"
        const val ARG_CATEGORY_ID = "categoryId"
    }
}

data class CategorySongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
