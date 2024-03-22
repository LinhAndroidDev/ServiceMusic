package com.example.serviceandroid.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
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

class CustomBottomBar : LinearLayout {
    private val binding by lazy { CustomBottomBarBinding.inflate(LayoutInflater.from(context)) }
    var selectedItem: ((ActionBottomBar) -> Unit)? = null

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initView() {
        binding.root.layoutParams = LayoutParams(
            Convert.getWidthDevice(context),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addView(binding.root)

        binding.bgBottom.setOnTouchListener { _, _ -> true }
        handlerActionBottomBar(ActionBottomBar.DISCOVER)
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

    private fun handlerActionBottomBar(action: ActionBottomBar) {
        when (action) {
            ActionBottomBar.LIBRARY -> {
                selectedItem?.invoke(ActionBottomBar.LIBRARY)
                unSelectAllItem()
                binding.tvLibrary.setTextColor(context.getColor(R.color.bg_purple))
                binding.tvLibrary.typeface = Typeface.DEFAULT_BOLD
                binding.imgLibrary.setColorFilter(context.getColor(R.color.bg_purple))
            }

            ActionBottomBar.DISCOVER -> {
                selectedItem?.invoke(ActionBottomBar.DISCOVER)
                unSelectAllItem()
                binding.tvDiscover.setTextColor(context.getColor(R.color.bg_purple))
                binding.tvDiscover.typeface = Typeface.DEFAULT_BOLD
                binding.imgDiscover.setColorFilter(context.getColor(R.color.bg_purple))
            }

            ActionBottomBar.ZINGCHART -> {
                selectedItem?.invoke(ActionBottomBar.ZINGCHART)
                unSelectAllItem()
                binding.tvZingchart.setTextColor(context.getColor(R.color.bg_purple))
                binding.tvZingchart.typeface = Typeface.DEFAULT_BOLD
                binding.imgZingchart.setColorFilter(context.getColor(R.color.bg_purple))
            }

            ActionBottomBar.RADIO -> {
                selectedItem?.invoke(ActionBottomBar.RADIO)
                unSelectAllItem()
                binding.tvRadio.setTextColor(context.getColor(R.color.bg_purple))
                binding.tvRadio.typeface = Typeface.DEFAULT_BOLD
                binding.imgRadio.setColorFilter(context.getColor(R.color.bg_purple))
            }

            ActionBottomBar.PROFILE -> {
                selectedItem?.invoke(ActionBottomBar.PROFILE)
                unSelectAllItem()
                binding.tvProfile.setTextColor(context.getColor(R.color.bg_purple))
                binding.tvProfile.typeface = Typeface.DEFAULT_BOLD
                binding.imgProfile.setColorFilter(context.getColor(R.color.bg_purple))
            }
        }
    }

    private fun unSelectAllItem() {
        /**
         * UnSelect All Text View
         */
        binding.tvLibrary.setTextColor(context.getColor(R.color.txt_hint))
        binding.tvLibrary.typeface = Typeface.DEFAULT
        binding.tvDiscover.setTextColor(context.getColor(R.color.txt_hint))
        binding.tvDiscover.typeface = Typeface.DEFAULT
        binding.tvZingchart.setTextColor(context.getColor(R.color.txt_hint))
        binding.tvZingchart.typeface = Typeface.DEFAULT
        binding.tvRadio.setTextColor(context.getColor(R.color.txt_hint))
        binding.tvRadio.typeface = Typeface.DEFAULT
        binding.tvProfile.setTextColor(context.getColor(R.color.txt_hint))
        binding.tvProfile.typeface = Typeface.DEFAULT

        /**
         * UnSelect All ImageView
         */
        binding.imgLibrary.setColorFilter(context.getColor(R.color.txt_hint))
        binding.imgDiscover.setColorFilter(context.getColor(R.color.txt_hint))
        binding.imgZingchart.setColorFilter(context.getColor(R.color.txt_hint))
        binding.imgRadio.setColorFilter(context.getColor(R.color.txt_hint))
        binding.imgProfile.setColorFilter(context.getColor(R.color.txt_hint))
    }
}