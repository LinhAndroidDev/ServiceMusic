package com.example.serviceandroid.lyrics

/** One timed line from an LRC-style `.txt` file (`[mm:ss.xx] text`). */
data class TimedLyricLine(
    val startSec: Double,
    val text: String,
)
