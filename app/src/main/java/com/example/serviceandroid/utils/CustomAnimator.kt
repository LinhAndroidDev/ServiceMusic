package com.example.serviceandroid.utils

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import com.example.serviceandroid.helper.Constants
import de.hdodenhof.circleimageview.CircleImageView

object CustomAnimator {
    fun rotationImage(img: CircleImageView) {
        val rotationAnimator = ObjectAnimator.ofFloat(img, "rotation", 0f, 360f)
        rotationAnimator.apply {
            duration = Constants.TIME_ROTATE
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }
    }

    fun endAnimation(anim: Animation, end: () -> Unit) {
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                end()
            }

            override fun onAnimationRepeat(p0: Animation?) {}

        })
    }
}