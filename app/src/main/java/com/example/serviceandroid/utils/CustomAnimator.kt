package com.example.serviceandroid.utils

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
}