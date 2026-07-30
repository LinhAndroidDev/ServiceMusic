package com.example.serviceandroid.fragment.downloaded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.DownloadedSongRepository
import com.example.serviceandroid.download.DownloadScheduler
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadedSongsViewModel @Inject constructor(
    private val downloadedSongRepository: DownloadedSongRepository,
    private val downloadScheduler: DownloadScheduler,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = downloadedSongRepository.observeCompletedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val count: StateFlow<Int> = downloadedSongRepository.observeCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun deleteSong(songId: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        downloadScheduler.cancel(songId)
        downloadedSongRepository.deleteDownload(songId)
        onDone()
    }
}
