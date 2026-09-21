package com.example.serviceandroid.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerFormatterTest {

    @Test
    fun formatRemainingDuration_roundsUpToMinutes() {
        assertEquals("0 phút", SleepTimerFormatter.formatRemainingDuration(0))
        assertEquals("1 phút", SleepTimerFormatter.formatRemainingDuration(1))
        assertEquals("15 phút", SleepTimerFormatter.formatRemainingDuration(15 * 60_000L))
        assertEquals("1 giờ", SleepTimerFormatter.formatRemainingDuration(60 * 60_000L))
        assertEquals(
            "1 giờ 5 phút",
            SleepTimerFormatter.formatRemainingDuration((65 * 60_000L) - 1),
        )
    }

    @Test
    fun formatCancelLabel_includesRemaining() {
        assertEquals(
            "Tắt hẹn giờ (Còn lại 15 phút)",
            SleepTimerFormatter.formatCancelLabel(15 * 60_000L),
        )
    }

    @Test
    fun remainingMs_usesDeadlineOrTrack() {
        val countdown = SleepTimerState(
            option = SleepTimerOption.MIN_15,
            endsAtElapsedRealtime = 20_000L,
        )
        assertEquals(5_000L, countdown.remainingMs(nowElapsedRealtime = 15_000L))

        val endOfTrack = SleepTimerState(
            option = SleepTimerOption.END_OF_TRACK,
            stopAtEndOfTrack = true,
        )
        assertEquals(
            12_000L,
            endOfTrack.remainingMs(nowElapsedRealtime = 0L, trackRemainingMs = 12_000L),
        )
    }
}
