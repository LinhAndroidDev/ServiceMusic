package com.example.serviceandroid.custom

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.LayoutDialogSleepTimerCustomBinding
import androidx.core.graphics.drawable.toDrawable

class DialogSleepTimerCustom : DialogFragment() {
    private var binding: LayoutDialogSleepTimerCustomBinding? = null
    var onConfirmDuration: ((Long) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = LayoutDialogSleepTimerCustomBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hoursPicker = binding?.hoursPicker ?: return
        val minutesPicker = binding?.minutesPicker ?: return
        hoursPicker.minValue = 0
        hoursPicker.maxValue = 11
        hoursPicker.value = 0
        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        minutesPicker.value = 15

        binding?.cancelDialog?.setOnClickListener { dismiss() }
        binding?.confirmTime?.setOnClickListener {
            val durationMs = (hoursPicker.value * 60 + minutesPicker.value) * 60_000L
            if (durationMs < 60_000L) {
                Toast.makeText(
                    requireContext(),
                    R.string.sleep_timer_custom_invalid,
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }
            onConfirmDuration?.invoke(durationMs)
            dismiss()
        }
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
