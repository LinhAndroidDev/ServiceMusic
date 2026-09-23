package com.example.serviceandroid.playback

object SleepTimerFormatter {
    fun formatRemainingDuration(remainingMs: Long): String {
        if (remainingMs <= 0L) return "0 phút"
        val totalMinutes = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0L && minutes > 0L -> "$hours giờ $minutes phút"
            hours > 0L -> "$hours giờ"
            else -> "$minutes phút"
        }
    }

    fun formatCancelLabel(remainingMs: Long): String =
        "Tắt hẹn giờ (Còn lại ${formatRemainingDuration(remainingMs)})"
}
