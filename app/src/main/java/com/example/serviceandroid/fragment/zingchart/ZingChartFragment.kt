package com.example.serviceandroid.fragment.zingchart

import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentZingChartBinding
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ZingChartFragment : BaseFragment<FragmentZingChartBinding>() {

    private var mediaRecorder: MediaRecorder? = null
    private var fileName: String = ""
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        } else {
            false
        }
        if (!permissionToRecordAccepted) activity?.finish()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        binding.header.title.text = "#zingchart"
        binding.header.title.let {
            val paint = it.paint
            val width = paint.measureText(it.text.toString())
            val textShader: Shader = LinearGradient(
                0f,
                0f,
                width,
                it.textSize,
                intArrayOf(Color.CYAN, Color.MAGENTA, Color.YELLOW),
                null,
                Shader.TileMode.CLAMP
            )
            it.paint.shader = textShader
        }

        initView()
    }

    private fun initView() {
        binding.recordButton.setOnClickListener {
            startRecording()
        }

        binding.stopButton.setOnClickListener {
            stopRecording()
        }
    }

    private fun startRecording() {
        fileName = "${activity?.externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        val file = File(fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        val audioData = getAudioData(fileName)
        binding.waveformView.setAudioData(audioData)
        startWaveformAnimation(audioData.size)
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(fileName)
            prepare()
            start()
        }
    }

    private fun startWaveformAnimation(dataSize: Int) {
        val animator = ValueAnimator.ofFloat(0f, dataSize.toFloat())
        animator.duration = 10000 // Duration in milliseconds
        animator.addUpdateListener { animation ->
            val offset = animation.animatedValue as Float
            binding.waveformView.setOffset(offset)
        }
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    fun getAudioData(filePath: String): ShortArray {
        val file = File(filePath)
        if (!file.exists()) {
            throw FileNotFoundException("File not found at path: $filePath")
        }
        val audioBytes = file.readBytes()
        val shortArray = ShortArray(audioBytes.size / 2)
        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
        return shortArray
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentZingChartBinding.inflate(inflater)

}