package com.example.serviceandroid.fragment.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.artist.FollowMutationResult
import com.example.serviceandroid.data.artist.FollowedSingerRepository
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.toDomainSinger
import com.example.serviceandroid.utils.VietnameseFold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddArtistUiState(
    val query: String = "",
    val singers: List<Singer> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)

sealed interface AddArtistCompleteResult {
    data object NoneSelected : AddArtistCompleteResult
    data object Success : AddArtistCompleteResult
    data class Failed(val result: FollowMutationResult) : AddArtistCompleteResult
}

@HiltViewModel
class AddArtistViewModel @Inject constructor(
    private val firestore: FirestoreMusicRepository,
    private val followedSingerRepository: FollowedSingerRepository,
) : ViewModel() {

    private var catalog: List<Singer> = emptyList()
    private val _uiState = MutableStateFlow(AddArtistUiState())
    val uiState: StateFlow<AddArtistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            catalog = runCatching {
                firestore.getSingers()
                    .map { it.toDomainSinger() }
                    .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            }.getOrDefault(emptyList())
            publish()
        }
    }

    fun onQueryChanged(raw: String) {
        _uiState.value = _uiState.value.copy(query = raw.trim())
        publish()
    }

    fun toggleSelection(singerId: String) {
        if (singerId.isBlank() || _uiState.value.isSaving) return
        val next = _uiState.value.selectedIds.toMutableSet()
        if (!next.add(singerId)) next.remove(singerId)
        _uiState.value = _uiState.value.copy(selectedIds = next)
    }

    fun complete(onResult: (AddArtistCompleteResult) -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return
        val selected = catalog.filter { it.id in state.selectedIds }
        if (selected.isEmpty()) {
            onResult(AddArtistCompleteResult.NoneSelected)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            for (singer in selected) {
                when (val result = followedSingerRepository.follow(singer)) {
                    FollowMutationResult.Success -> Unit
                    else -> {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                        onResult(AddArtistCompleteResult.Failed(result))
                        return@launch
                    }
                }
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
            onResult(AddArtistCompleteResult.Success)
        }
    }

    private fun publish() {
        val query = _uiState.value.query
        val visible = if (query.isBlank()) {
            catalog
        } else {
            catalog.filter { VietnameseFold.matches(it.name, query) }
        }
        _uiState.value = _uiState.value.copy(singers = visible, isLoading = false)
    }
}
