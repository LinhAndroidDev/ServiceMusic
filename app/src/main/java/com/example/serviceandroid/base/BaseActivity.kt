package com.example.serviceandroid.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.serviceandroid.R

@Suppress("DEPRECATION")
abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity(){
    lateinit var binding: VB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = getActivityBinding(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(R.anim.slide_up, R.anim.anim_normal)
        changeColorStatusBar(Color.WHITE)
    }

    fun changeColorStatusBar(color: Int) {
        window.statusBarColor = color
    }

    private fun fullScreen() {
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT
    }

    protected abstract fun getActivityBinding(inflater: LayoutInflater): VB

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.anim_normal, R.anim.slide_down)
    }
}