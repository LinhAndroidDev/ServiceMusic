package com.example.serviceandroid.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.DownloadStatus
import com.example.serviceandroid.database.repository.DownloadedSongRepository
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.download.DownloadScheduler
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BottomSheetOptionMusicViewModel @Inject constructor(
    private val repository: FavouriteSongRepository,
    private val downloadedSongRepository: DownloadedSongRepository,
    private val downloadScheduler: DownloadScheduler,
) : ViewModel() {
    private val _isFavourite: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFavourite get() = _isFavourite

    private val _downloadStatus = MutableStateFlow<DownloadStatus?>(null)
    val downloadStatus: StateFlow<DownloadStatus?> = _downloadStatus.asStateFlow()

    private var statusJob: Job? = null

    fun insertSong(song: Song, timeCreate: String, onCallBackInsertSong: () -> Unit) =
        viewModelScope.launch {
            repository.insertSong(song, timeCreate)
            onCallBackInsertSong.invoke()
        }

    fun deleteSongById(id: String, onCallBackDeleteSong: () -> Unit) = viewModelScope.launch {
        repository.deleteSongById(id)
        onCallBackDeleteSong.invoke()
    }

    fun checkSongById(id: String) = viewModelScope.launch {
        _isFavourite.value = repository.checkSongById(id)
    }

    fun observeDownload(songId: String) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            downloadedSongRepository.observeStatus(songId).collect { status ->
                _downloadStatus.value = status
            }
        }
    }

    fun enqueueDownload(song: Song, onStarted: () -> Unit, onAlreadyDownloaded: () -> Unit) =
        viewModelScope.launch {
            val existing = downloadedSongRepository.getById(song.id)
            if (existing?.status == DownloadStatus.COMPLETED) {
                onAlreadyDownloaded()
                return@launch
            }
            downloadScheduler.enqueue(song)
            onStarted()
        }

    fun removeDownload(songId: String, onDone: () -> Unit) = viewModelScope.launch {
        downloadScheduler.cancel(songId)
        downloadedSongRepository.deleteDownload(songId)
        onDone()
    }
}
