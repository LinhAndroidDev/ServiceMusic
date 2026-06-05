package com.example.serviceandroid.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val firestore: FirestoreMusicRepository,
) : ViewModel() {

    private val _results = MutableStateFlow<List<Song>>(emptyList())
    val results: StateFlow<List<Song>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val songs = runCatching {
                firestore.searchSongsByTitle(query).map { it.toDomainSong() }
            }.getOrDefault(emptyList())
            _results.value = songs
            _isSearching.value = false
        }
    }

    fun clearResults() {
        _results.value = emptyList()
    }
}
