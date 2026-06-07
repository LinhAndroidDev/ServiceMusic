package com.example.serviceandroid.fragment.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.model.Advertisement
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainAdvertisement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FavouriteSongRepository,
    private val songRepository: SongRepository,
    private val firestoreMusicRepository: FirestoreMusicRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _advertisements = MutableStateFlow<List<Advertisement>>(emptyList())
    val advertisements: StateFlow<List<Advertisement>> = _advertisements.asStateFlow()

    init {
        ensureLoaded()
        loadAdvertisements()
    }

    fun ensureLoaded() {
        if (_playlist.value.isNotEmpty()) {
            _isLoading.value = false
            return
        }
        val cached = songRepository.getLatestPlaylist()
        if (cached.isNotEmpty()) {
            _playlist.value = cached
            _isLoading.value = false
            return
        }
        loadPlaylist()
    }

    fun loadPlaylist(force: Boolean = false) {
        if (!force && _playlist.value.isNotEmpty()) return
        if (!force) {
            val cached = songRepository.getLatestPlaylist()
            if (cached.isNotEmpty()) {
                _playlist.value = cached
                _isLoading.value = false
                return
            }
        }
        viewModelScope.launch {
            _isLoading.value = true
            songRepository.refreshPlaylist()
            _playlist.value = songRepository.getLatestPlaylist()
            _isLoading.value = false
        }
    }

    fun loadAdvertisements(force: Boolean = false) {
        if (!force && _advertisements.value.isNotEmpty()) return
        viewModelScope.launch {
            if (force) firestoreMusicRepository.invalidateAdvertisementCache()
            val ads = runCatching {
                firestoreMusicRepository.getAdvertisements()
                    .map { it.toDomainAdvertisement() }
            }.getOrDefault(emptyList())
            _advertisements.value = ads
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                coroutineScope {
                    val playlistJob = async {
                        songRepository.refreshPlaylist()
                        songRepository.getLatestPlaylist()
                    }
                    val adsJob = async {
                        firestoreMusicRepository.invalidateAdvertisementCache()
                        runCatching {
                            firestoreMusicRepository.getAdvertisements()
                                .map { it.toDomainAdvertisement() }
                        }.getOrDefault(emptyList())
                    }
                    _playlist.value = playlistJob.await()
                    _advertisements.value = adsJob.await()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun getPlaylist(): List<Song> = _playlist.value.ifEmpty { songRepository.getLatestPlaylist() }

    fun deleteSongById(id: String, onCallBackDeleteSong: () -> Unit) = viewModelScope.launch {
        repository.deleteSongById(id)
        onCallBackDeleteSong.invoke()
    }
}
