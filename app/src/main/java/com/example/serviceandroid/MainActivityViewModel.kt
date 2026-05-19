package com.example.serviceandroid

import androidx.lifecycle.ViewModel
import com.example.serviceandroid.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val shared: SharePreferenceRepository,
) : ViewModel() {

    fun getTypeRepeat() = shared.getTypeRepeat()
}