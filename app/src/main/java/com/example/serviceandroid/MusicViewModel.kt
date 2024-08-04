package com.example.serviceandroid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(private val repository: FavouriteSongRepository, @ApplicationContext private val context: Context) :
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

    suspend fun checkSongById(id: Int) {
        _isFavourite.value = repository.checkSongById(id)
    }
}