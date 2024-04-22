package com.example.serviceandroid.fragment.favourite_song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentFavouriteSongViewModel @Inject constructor(private val repository: FavouriteSongRepository) : ViewModel() {

    suspend fun getAll(): MutableList<Song>? = repository.getAll()

    fun insertSong(song: Song) = viewModelScope.launch {
        repository.insertSong(song)
    }

    fun deleteSongById(id: Int) = viewModelScope.launch {
        repository.deleteSongById(id)
    }
}