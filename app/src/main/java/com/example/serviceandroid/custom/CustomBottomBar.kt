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
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.CustomBottomBarBinding
import com.example.serviceandroid.utils.Convert

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
            Convert.getWidthDevice(context),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addView(binding.root)
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.CustomBottomBar, 0, 0)
        val action = array.getInt(R.styleable.CustomBottomBar_item_selected, 0)
        handlerActionBottomBar(ActionBottomBar.entries[action])
        binding.bgBottom.setOnTouchListener { _, _ -> true }
        onClickView()
    }

    private fun handlerActionBottomBar(action: ActionBottomBar) {
        when (action) {
            ActionBottomBar.LIBRARY -> {
                selectedItem?.invoke(ActionBottomBar.LIBRARY)
                selectedItem(binding.tvLibrary, binding.imgLibrary)
            }

            ActionBottomBar.DISCOVER -> {
                selectedItem?.invoke(ActionBottomBar.DISCOVER)
                selectedItem(binding.tvDiscover, binding.imgDiscover)
            }

            ActionBottomBar.ZINGCHART -> {
                selectedItem?.invoke(ActionBottomBar.ZINGCHART)
                selectedItem(binding.tvZingchart, binding.imgZingchart)
            }

            ActionBottomBar.RADIO -> {
                selectedItem?.invoke(ActionBottomBar.RADIO)
                selectedItem(binding.tvRadio, binding.imgRadio)
            }

            ActionBottomBar.PROFILE -> {
                selectedItem?.invoke(ActionBottomBar.PROFILE)
                selectedItem(binding.tvProfile, binding.imgProfile)
            }
        }
    }

    private fun onClickView() {
        binding.library.setOnClickListener {
            handlerActionBottomBar(ActionBottomBar.LIBRARY)
        }
        binding.discover.setOnClickListener {
            handlerActionBottomBar(ActionBottomBar.DISCOVER)
        }
        binding.zingchart.setOnClickListener {
            handlerActionBottomBar(ActionBottomBar.ZINGCHART)
        }
        binding.radio.setOnClickListener {
            handlerActionBottomBar(ActionBottomBar.RADIO)
        }
        binding.profile.setOnClickListener {
            handlerActionBottomBar(ActionBottomBar.PROFILE)
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
        unSelectAllItem()
        tv.setTextColor(context.getColor(R.color.bg_purple))
        tv.typeface = Typeface.DEFAULT_BOLD
        img.setColorFilter(context.getColor(R.color.bg_purple))
    }

    private fun unSelectedItem(tv: TextView, img: ImageView) {
        tv.setTextColor(context.getColor(R.color.txt_hint))
        tv.typeface = Typeface.DEFAULT
        img.setColorFilter(context.getColor(R.color.txt_hint))
    }
}