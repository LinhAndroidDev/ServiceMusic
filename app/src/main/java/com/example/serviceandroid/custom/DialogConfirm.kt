package com.example.serviceandroid.custom

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.LayoutDialogConfirmBinding
import androidx.core.graphics.drawable.toDrawable

@SuppressLint("UseGetLayoutInflater", "InflateParams")
class DialogConfirm : DialogFragment() {
    private var v: LayoutDialogConfirmBinding? = null
    var title = ""
    var message: String? = null
    var confirmText: String? = null
    var cancelText: String? = null
    var onClickRemove: (() -> Unit)? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v = LayoutDialogConfirmBinding.inflate(inflater, container, false)
        return v?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        v?.titleDialog?.text = title
        v?.messageDialog?.text =
            message ?: getString(R.string.delete_song_from_library_message)
        v?.removeSong?.text = confirmText ?: getString(R.string.action_delete)
        v?.cancelDialog?.text = cancelText ?: getString(R.string.action_cancel)
        v?.cancelDialog?.setOnClickListener { dismiss() }
        v?.removeSong?.setOnClickListener {
            onClickRemove?.invoke()
            dismiss()
        }
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }
}