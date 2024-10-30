package com.example.serviceandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(private val repository: FavouriteSongRepository) :
    ViewModel() {
    private val _isFavourite: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFavourite get() = _isFavourite

    fun insertSong(song: Song, timeCreate: String, onCallBackInsertSong: () -> Unit) =
        viewModelScope.launch {
            repository.insertSong(song, timeCreate)
            onCallBackInsertSong.invoke()
        }

    fun deleteSongById(id: Int, onCallBackDeleteSong: () -> Unit) = viewModelScope.launch {
        repository.deleteSongById(id)
        onCallBackDeleteSong.invoke()
    }

    fun checkSongById(id: Int) = viewModelScope.launch {
        _isFavourite.value = repository.checkSongById(id)
    }
}