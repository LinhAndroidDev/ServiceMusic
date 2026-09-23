package com.example.serviceandroid.custom

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
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
    var onQueryChanged: ((String) -> Unit)? = null
    var onSearchAction: ((String) -> Unit)? = null
    private var suppressQueryCallback: Boolean = false

    init {
        binding = CustomSearchViewBinding.inflate(LayoutInflater.from(context))
        addView(binding?.root)
        initView()
    }

    private fun initView() {
        binding?.search?.doOnTextChanged { text, _, _, _ ->
            binding?.removeText?.isVisible = text?.isNotEmpty() == true
            if (!suppressQueryCallback) {
                onQueryChanged?.invoke(text?.toString().orEmpty())
            }
        }

        binding?.removeText?.setOnClickListener {
            binding?.search?.setText("")
        }

        binding?.search?.setOnEditorActionListener { _, actionId, event ->
            val isSearch = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (isSearch) {
                onSearchAction?.invoke(queryText())
                true
            } else {
                false
            }
        }
    }

    fun queryText(): String = binding?.search?.text?.toString().orEmpty()

    /** Restores text without notifying [onQueryChanged] (used when returning from another screen). */
    fun setQuery(text: String, notify: Boolean = true) {
        val edit = binding?.search ?: return
        if (!notify) suppressQueryCallback = true
        if (edit.text?.toString() != text) {
            edit.setText(text)
            edit.setSelection(text.length.coerceAtMost(edit.text?.length ?: 0))
        }
        binding?.removeText?.isVisible = text.isNotEmpty()
        suppressQueryCallback = false
        if (notify) {
            onQueryChanged?.invoke(text)
        }
    }

    fun showActionSearch() {
        binding?.search?.isFocusable = true
        binding?.search?.showKeyboard()
    }
}
