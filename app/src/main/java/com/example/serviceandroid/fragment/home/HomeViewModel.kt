package com.example.serviceandroid.fragment.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.R
import com.example.serviceandroid.data.firestore.FirestoreCategory
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.model.Advertisement
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.Topic
import com.example.serviceandroid.model.TopicType
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
    private val songRepository: SongRepository,
    private val firestoreMusicRepository: FirestoreMusicRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _topSongs = MutableStateFlow<List<Song>>(emptyList())
    val topSongs: StateFlow<List<Song>> = _topSongs.asStateFlow()

    private val _advertisements = MutableStateFlow<List<Advertisement>>(emptyList())
    val advertisements: StateFlow<List<Advertisement>> = _advertisements.asStateFlow()

    private val _topics = MutableStateFlow(shortcutTopics())
    val topics: StateFlow<List<Topic>> = _topics.asStateFlow()

    init {
        ensureLoaded()
        ensureTopSongsLoaded()
        loadAdvertisements()
        loadTopics()
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
                        songRepository.refreshPlaylist().getOrThrow()
                        songRepository.getLatestPlaylist()
                    }
                    val adsJob = async {
                        firestoreMusicRepository.invalidateAdvertisementCache()
                        runCatching {
                            firestoreMusicRepository.getAdvertisements(fromServer = true)
                                .map { it.toDomainAdvertisement() }
                        }.getOrDefault(emptyList())
                    }
                    val topicsJob = async { fetchTopics() }
                    val topSongsJob = async {
                        songRepository.refreshTopPlaylist().getOrThrow()
                        songRepository.getTopPlaylist()
                    }
                    _playlist.value = playlistJob.await()
                    _advertisements.value = adsJob.await()
                    _topics.value = topicsJob.await()
                    _topSongs.value = topSongsJob.await()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun getPlaylist(): List<Song> = _playlist.value.ifEmpty { songRepository.getLatestPlaylist() }

    fun getTopSongs(): List<Song> = _topSongs.value.ifEmpty { songRepository.getTopPlaylist() }

    fun prepareTopPlaybackQueue() {
        val songs = getTopSongs()
        if (songs.isNotEmpty()) {
            songRepository.setPlaybackQueue(songs)
        }
    }

    fun getAdvertisements(): List<Advertisement> = _advertisements.value

    fun ensureTopSongsLoaded() {
        if (_topSongs.value.isNotEmpty()) return
        val cached = songRepository.getTopPlaylist()
        if (cached.isNotEmpty()) {
            _topSongs.value = cached
            return
        }
        viewModelScope.launch {
            songRepository.refreshTopPlaylist()
            _topSongs.value = songRepository.getTopPlaylist()
        }
    }

    fun loadTopics() {
        viewModelScope.launch {
            _topics.value = fetchTopics()
        }
    }

    private suspend fun fetchTopics(): List<Topic> {
        val categories = runCatching {
            firestoreMusicRepository.getCategories()
        }.getOrDefault(emptyList())
        return shortcutTopics() + categoryTopics(categories) + Topic(type = TopicType.SEE_ALL)
    }

    private fun shortcutTopics(): List<Topic> = listOf(
        Topic(
            icon = R.drawable.ic_music,
            topic = "BXH Nhạc Mới",
            color = R.color.bg_blue,
            type = TopicType.NEW_CHART,
        ),
        Topic(
            icon = R.drawable.ic_star,
            topic = "Top 100",
            color = R.color.bg_purple,
            type = TopicType.TOP_100,
        ),
    )

    private fun categoryTopics(categories: List<FirestoreCategory>): List<Topic> {
        val colors = listOf(
            R.color.bg_orange,
            R.color.bg_pink,
            R.color.bg_green1,
            R.color.bg_green2,
        )
        return categories.mapIndexed { index, category ->
            Topic(
                topic = category.name,
                color = colors[index % colors.size],
                type = TopicType.CATEGORY,
                categoryId = category.id,
            )
        }
    }
}
