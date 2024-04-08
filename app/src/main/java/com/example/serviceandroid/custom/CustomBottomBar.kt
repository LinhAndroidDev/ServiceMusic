package com.example.serviceandroid.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.CustomBottomBarBinding

enum class ActionBottomBar {
    LIBRARY,
    DISCOVER,
    ZINGCHART,
    RADIO,
    PROFILE
}

@SuppressLint("ClickableViewAccessibility")
class CustomBottomBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null , defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = CustomBottomBarBinding.inflate(LayoutInflater.from(context))
    var selectedItem: ((ActionBottomBar) -> Unit)? = null
    init {
        binding.root.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addView(binding.root)
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.CustomBottomBar, 0, 0)
        val action = array.getInt(R.styleable.CustomBottomBar_item_selected, 0)
        handlerActionBottomBar(ActionBottomBar.entries[action])
        binding.bgBottom.apply {
            isClickable = true
            isFocusable = true
        }
        onClickView()
    }

    private fun handlerActionBottomBar(action: ActionBottomBar) {
        unSelectAllItem()
        val viewMap = mapOf(
            ActionBottomBar.LIBRARY to Pair(binding.tvLibrary, binding.imgLibrary),
            ActionBottomBar.DISCOVER to Pair(binding.tvDiscover, binding.imgDiscover),
            ActionBottomBar.ZINGCHART to Pair(binding.tvZingchart, binding.imgZingchart),
            ActionBottomBar.RADIO to Pair(binding.tvRadio, binding.imgRadio),
            ActionBottomBar.PROFILE to Pair(binding.tvProfile, binding.imgProfile)
        )
        viewMap[action]?.let { (tv, img) ->
            selectedItem?.invoke(action)
            selectedItem(tv, img)
        }
    }

    private fun onClickView() {
        val views = listOf(binding.library, binding.discover, binding.zingchart, binding.radio, binding.profile)
        views.forEach { view ->
            view.setOnClickListener {
                val action = when(view) {
                    binding.library -> ActionBottomBar.LIBRARY
                    binding.discover -> ActionBottomBar.DISCOVER
                    binding.zingchart -> ActionBottomBar.ZINGCHART
                    binding.radio -> ActionBottomBar.RADIO
                    binding.profile -> ActionBottomBar.PROFILE
                    else -> throw IllegalArgumentException("Invalid view")
                }
                handlerActionBottomBar(action)
            }
        }
    }

    private fun unSelectAllItem() {
        unSelectedItem(binding.tvLibrary, binding.imgLibrary)
        unSelectedItem(binding.tvDiscover, binding.imgDiscover)
        unSelectedItem(binding.tvZingchart, binding.imgZingchart)
        unSelectedItem(binding.tvRadio, binding.imgRadio)
        unSelectedItem(binding.tvProfile, binding.imgProfile)
    }

    private fun selectedItem(tv: TextView, img: ImageView) {
        tv.setTextColor(ContextCompat.getColor(context, R.color.bg_purple))
        tv.typeface = Typeface.DEFAULT_BOLD
        img.setColorFilter(ContextCompat.getColor(context, R.color.bg_purple))
    }

    private fun unSelectedItem(tv: TextView, img: ImageView) {
        tv.setTextColor(ContextCompat.getColor(context, R.color.txt_hint))
        tv.typeface = Typeface.DEFAULT
        img.setColorFilter(ContextCompat.getColor(context, R.color.txt_hint))
    }
}