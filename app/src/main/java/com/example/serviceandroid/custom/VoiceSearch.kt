package com.example.serviceandroid.custom

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.serviceandroid.R

class VoiceSearch(
    private val fragment: Fragment,
    private val onResult: (String) -> Unit,
) {
    private val permissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showDialog()
        } else {
            val context = fragment.context ?: return@registerForActivityResult
            Toast.makeText(
                context,
                context.getString(R.string.voice_search_permission_denied),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun start() {
        val context = fragment.context ?: return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            showDialog()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun showDialog() {
        if (!fragment.isAdded) return
        val manager = fragment.childFragmentManager
        if (manager.findFragmentByTag(TAG) != null) return
        VoiceSearchDialog().apply {
            onResult = this@VoiceSearch.onResult
        }.show(manager, TAG)
    }

    private companion object {
        const val TAG = "VoiceSearchDialog"
    }
}
