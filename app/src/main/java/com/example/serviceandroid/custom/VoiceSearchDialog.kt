package com.example.serviceandroid.custom

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.LayoutDialogVoiceSearchBinding
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable

class VoiceSearchDialog : DialogFragment() {
    private var binding: LayoutDialogVoiceSearchBinding? = null
    private var recognizer: SpeechRecognizer? = null
    private var delivered = false
    private var closing = false

    var onResult: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = LayoutDialogVoiceSearchBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        binding?.cancelDialog?.setOnClickListener { dismiss() }
        startListening()
    }

    private fun startListening() {
        val context = context ?: return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            setStatus(R.string.voice_search_unsupported)
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { engine ->
                engine.setRecognitionListener(recognitionListener)
            }
        }
        setStatus(R.string.voice_search_listening)
        recognizer?.startListening(recognitionIntent())
    }

    private fun recognitionIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, VI_LOCALE.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, VI_LOCALE.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults)
            if (text.isNotBlank()) {
                binding?.statusDialog?.text = text
            }
        }

        override fun onResults(results: Bundle?) {
            if (closing || delivered) return
            val text = firstResult(results)
            if (text.isBlank()) {
                setStatus(R.string.voice_search_try_again)
                restartListening()
                return
            }
            delivered = true
            onResult?.invoke(text)
            dismissAllowingStateLoss()
        }

        override fun onError(error: Int) {
            if (closing || delivered || !isAdded) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> {
                    setStatus(R.string.voice_search_try_again)
                    restartListening()
                }
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                -> setStatus(R.string.voice_search_need_network)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                -> setStatus(R.string.voice_search_permission_denied)
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_SERVER,
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                -> setStatus(R.string.voice_search_unsupported)
                else -> {
                    setStatus(R.string.voice_search_try_again)
                    restartListening()
                }
            }
        }
    }

    private fun firstResult(bundle: Bundle?): String =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

    private fun setStatus(resId: Int) {
        binding?.statusDialog?.setText(resId)
    }

    private fun restartListening() {
        val root = view ?: return
        root.removeCallbacks(restartRunnable)
        root.postDelayed(restartRunnable, RESTART_DELAY_MS)
    }

    private val restartRunnable = Runnable {
        if (!closing && !delivered && isAdded) {
            startListening()
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        closing = true
        view?.removeCallbacks(restartRunnable)
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        closing = true
        view?.removeCallbacks(restartRunnable)
        recognizer?.setRecognitionListener(null)
        runCatching { recognizer?.cancel() }
        recognizer?.destroy()
        recognizer = null
        binding = null
        super.onDestroyView()
    }

    private companion object {
        val VI_LOCALE: Locale = Locale.forLanguageTag("vi-VN")
        const val RESTART_DELAY_MS = 400L
    }
}
