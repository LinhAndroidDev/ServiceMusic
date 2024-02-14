package com.example.serviceandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.serviceandroid.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    companion object {
        const val MESSAGE_MAIN = "MESSAGE_MAIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.startMusic.setOnClickListener {
            clickStartService()
        }

        binding.stopMusic.setOnClickListener {
            clickStopService()
        }
    }

    private fun clickStopService() {
        val intent = Intent(this, HelloService::class.java)
        stopService(intent)
    }

    private fun clickStartService() {
        val intent = Intent(this, HelloService::class.java)
        val song = Song("Lạ Lùng", "Vũ", R.drawable.vu, R.raw.la_lung)
        intent.putExtra(MESSAGE_MAIN, song)
        startService(intent)
    }
}