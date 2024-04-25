package com.example.serviceandroid.custom

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.serviceandroid.databinding.LayoutDialogConfirmBinding

@SuppressLint("UseGetLayoutInflater", "InflateParams")
class DialogConfirm : DialogFragment() {
    private var v: LayoutDialogConfirmBinding? = null
    var title = ""
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
        v?.cancelDialog?.setOnClickListener { dismiss() }
        v?.removeSong?.setOnClickListener {
            onClickRemove?.invoke()
            dismiss()
        }
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}