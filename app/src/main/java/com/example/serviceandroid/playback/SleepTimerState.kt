package com.example.serviceandroid.playback

data class SleepTimerState(
    val option: SleepTimerOption? = null,
    val endsAtElapsedRealtime: Long? = null,
    val stopAtEndOfTrack: Boolean = false,
) {
    val isActive: Boolean get() = option != null

    fun remainingMs(
        nowElapsedRealtime: Long,
        trackRemainingMs: Long = 0L,
    ): Long {
        if (!isActive) return 0L
        if (stopAtEndOfTrack) return trackRemainingMs.coerceAtLeast(0L)
        val end = endsAtElapsedRealtime ?: return 0L
        return (end - nowElapsedRealtime).coerceAtLeast(0L)
    }

    companion object {
        fun idle() = SleepTimerState()
    }
}
