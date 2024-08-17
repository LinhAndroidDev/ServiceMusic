package com.example.serviceandroid.custom

import androidx.lifecycle.ViewModel
import com.example.serviceandroid.database.repository.ArrangeMusic
import com.example.serviceandroid.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BottomSheetSongArrangementViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    fun saveStateArrange(type: ArrangeMusic) {
        shared.saveTypeArrangement(type.ordinal)
    }
}