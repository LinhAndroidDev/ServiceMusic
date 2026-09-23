package com.example.serviceandroid.custom

import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment
import com.example.serviceandroid.databinding.LayoutBottomSheetSleepTimerBinding
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.playback.SleepTimerFormatter
import com.example.serviceandroid.playback.SleepTimerOption
import com.example.serviceandroid.playback.SleepTimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetSleepTimer :
    BaseBottomSheetDialogFragment<LayoutBottomSheetSleepTimerBinding>() {

    private val playbackViewModel by activityViewModels<PlaybackViewModel>()

    override val layoutResId: Int
        get() = R.layout.layout_bottom_sheet_sleep_timer

    override fun initView() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    playbackViewModel.sleepTimerState,
                    playbackViewModel.playbackState,
                    tickerFlow(),
                ) { timer, playback, _ ->
                    val trackRemaining = (playback.durationMs - playback.positionMs)
                        .coerceAtLeast(0)
                        .toLong()
                    timer to timer.remainingMs(
                        nowElapsedRealtime = SystemClock.elapsedRealtime(),
                        trackRemainingMs = trackRemaining,
                    )
                }.collect { (timer, remainingMs) ->
                    bindOptions(timer, remainingMs)
                }
            }
        }
    }

    override fun onClickView() {
        binding.option15.setOnClickListener { onOptionClicked(SleepTimerOption.MIN_15) }
        binding.option30.setOnClickListener { onOptionClicked(SleepTimerOption.MIN_30) }
        binding.option45.setOnClickListener { onOptionClicked(SleepTimerOption.MIN_45) }
        binding.option60.setOnClickListener { onOptionClicked(SleepTimerOption.HOUR_1) }
        binding.optionEndOfTrack.setOnClickListener {
            onOptionClicked(SleepTimerOption.END_OF_TRACK)
        }
        binding.optionCustom.setOnClickListener { onOptionClicked(SleepTimerOption.CUSTOM) }
    }

    private fun onOptionClicked(option: SleepTimerOption) {
        val current = playbackViewModel.sleepTimerState.value
        if (current.option == option) {
            playbackViewModel.cancelSleepTimer(requireContext())
            dismiss()
            return
        }
        if (option == SleepTimerOption.CUSTOM) {
            DialogSleepTimerCustom().apply {
                onConfirmDuration = { durationMs ->
                    playbackViewModel.setSleepTimer(
                        requireContext(),
                        SleepTimerOption.CUSTOM,
                        durationMs,
                    )
                    this@BottomSheetSleepTimer.dismiss()
                }
            }.show(parentFragmentManager, "sleep_timer_custom")
            return
        }
        playbackViewModel.setSleepTimer(requireContext(), option)
        dismiss()
    }

    private fun bindOptions(timer: SleepTimerState, remainingMs: Long) {
        bindOption(
            binding.tvOption15,
            R.string.sleep_timer_15,
            SleepTimerOption.MIN_15,
            timer,
            remainingMs,
        )
        bindOption(
            binding.tvOption30,
            R.string.sleep_timer_30,
            SleepTimerOption.MIN_30,
            timer,
            remainingMs,
        )
        bindOption(
            binding.tvOption45,
            R.string.sleep_timer_45,
            SleepTimerOption.MIN_45,
            timer,
            remainingMs,
        )
        bindOption(
            binding.tvOption60,
            R.string.sleep_timer_60,
            SleepTimerOption.HOUR_1,
            timer,
            remainingMs,
        )
        bindOption(
            binding.tvOptionEndOfTrack,
            R.string.sleep_timer_end_of_track,
            SleepTimerOption.END_OF_TRACK,
            timer,
            remainingMs,
        )
        bindOption(
            binding.tvOptionCustom,
            R.string.sleep_timer_custom,
            SleepTimerOption.CUSTOM,
            timer,
            remainingMs,
        )
    }

    private fun bindOption(
        label: TextView,
        defaultRes: Int,
        option: SleepTimerOption,
        timer: SleepTimerState,
        remainingMs: Long,
    ) {
        label.text = if (timer.option == option) {
            cancelLabel(remainingMs)
        } else {
            getString(defaultRes)
        }
    }

    private fun cancelLabel(remainingMs: Long): CharSequence {
        val remaining = SleepTimerFormatter.formatRemainingDuration(remainingMs)
        val full = getString(R.string.sleep_timer_cancel_remaining, remaining)
        val highlight = getString(R.string.sleep_timer_remaining_text, remaining)
        val start = full.indexOf(highlight)
        if (start < 0) return full
        val purple = ContextCompat.getColor(requireContext(), R.color.purple_1)
        return SpannableString(full).apply {
            setSpan(
                ForegroundColorSpan(purple),
                start,
                start + highlight.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    private fun tickerFlow() = flow {
        while (true) {
            emit(Unit)
            delay(1_000)
        }
    }
}
