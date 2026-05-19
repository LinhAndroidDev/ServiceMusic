package com.example.serviceandroid.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _navigateHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateHome: SharedFlow<Unit> = _navigateHome.asSharedFlow()

    private var splashJob: Job? = null

    fun restartSplashTimer(delayMs: Long = 2000L) {
        splashJob?.cancel()
        splashJob = viewModelScope.launch {
            delay(delayMs)
            _navigateHome.emit(Unit)
        }
    }

    fun cancelSplashTimer() {
        splashJob?.cancel()
        splashJob = null
    }
}
