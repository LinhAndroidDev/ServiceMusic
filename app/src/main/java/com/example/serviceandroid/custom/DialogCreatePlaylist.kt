package com.example.serviceandroid.custom

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.LayoutDialogCreatePlaylistBinding

class DialogCreatePlaylist : DialogFragment() {
    private var binding: LayoutDialogCreatePlaylistBinding? = null
    var onConfirm: ((title: String, isPublic: Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = LayoutDialogCreatePlaylistBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.switchPublic?.isChecked = true
        binding?.cancelDialog?.setOnClickListener { dismiss() }
        binding?.confirmCreate?.setOnClickListener {
            val title = binding?.edtPlaylistName?.text?.toString().orEmpty().trim()
            if (title.isBlank()) {
                Toast.makeText(requireContext(), R.string.playlist_name_required, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            onConfirm?.invoke(title, binding?.switchPublic?.isChecked != false)
            dismiss()
        }
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        focusPlaylistName()
    }

    private fun focusPlaylistName() {
        val name = binding?.edtPlaylistName ?: return
        name.requestFocus()
        name.post {
            val window = dialog?.window ?: return@post
            if (!name.isFocused) name.requestFocus()
            WindowCompat.getInsetsController(window, name).show(WindowInsetsCompat.Type.ime())
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
