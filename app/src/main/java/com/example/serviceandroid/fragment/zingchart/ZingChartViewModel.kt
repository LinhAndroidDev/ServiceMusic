package com.example.serviceandroid.fragment.zingchart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZingChartViewModel @Inject constructor(
    private val repository: FavouriteSongRepository,
    private val songRepository: SongRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    init {
        ensureLoaded()
    }

    /** Uses ViewModel cache, then repository top cache, then network (once). */
    fun ensureLoaded() {
        if (_playlist.value.isNotEmpty()) {
            _isLoading.value = false
            return
        }
        val cached = songRepository.getTopPlaylist()
        if (cached.isNotEmpty()) {
            _playlist.value = cached
            _isLoading.value = false
            return
        }
        loadTopSongs()
    }

    fun loadTopSongs(force: Boolean = false) {
        if (!force && _playlist.value.isNotEmpty()) return
        if (!force) {
            val cached = songRepository.getTopPlaylist()
            if (cached.isNotEmpty()) {
                _playlist.value = cached
                _isLoading.value = false
                return
            }
        }
        viewModelScope.launch {
            _isLoading.value = true
            songRepository.refreshTopPlaylist()
            _playlist.value = songRepository.getTopPlaylist()
            _isLoading.value = false
        }
    }

    fun getPlaylist(): List<Song> = _playlist.value.ifEmpty { songRepository.getTopPlaylist() }

    fun deleteSongById(id: String, onCallBackDeleteSong: () -> Unit) = viewModelScope.launch {
        repository.deleteSongById(id)
        onCallBackDeleteSong.invoke()
    }
}
