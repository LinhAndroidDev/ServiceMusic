package com.example.serviceandroid.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.example.serviceandroid.databinding.CustomSearchViewBinding
import com.example.serviceandroid.utils.ExtensionFunctions.showKeyboard

class CustomSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var binding: CustomSearchViewBinding? = null

    init {
        binding = CustomSearchViewBinding.inflate(LayoutInflater.from(context))
        addView(binding?.root)
        initView()
    }

    private fun initView() {
        binding?.search?.doOnTextChanged { text, _, _, _ ->
            binding?.removeText?.isVisible = text?.isNotEmpty() == true
        }

        binding?.removeText?.setOnClickListener {
            binding?.search?.setText("")
        }
    }

    fun showActionSearch() {
        binding?.search?.isFocusable = true
        binding?.search?.showKeyboard()
    }
}