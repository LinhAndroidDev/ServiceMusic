package com.example.serviceandroid.playback

enum class SleepTimerOption {
    MIN_15,
    MIN_30,
    MIN_45,
    HOUR_1,
    END_OF_TRACK,
    CUSTOM,
    ;

    fun presetDurationMs(): Long? = when (this) {
        MIN_15 -> 15 * 60_000L
        MIN_30 -> 30 * 60_000L
        MIN_45 -> 45 * 60_000L
        HOUR_1 -> 60 * 60_000L
        END_OF_TRACK, CUSTOM -> null
    }
}
