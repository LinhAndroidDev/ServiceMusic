package com.example.serviceandroid.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import com.example.serviceandroid.R
import com.example.serviceandroid.helper.Constants
import de.hdodenhof.circleimageview.CircleImageView

object CustomAnimator {

    /**
     * Continuous rotation for the cover art. Uses a hardware layer and reuses one animator
     * to reduce jank when the rest of the player UI (e.g. SeekBar) invalidates frequently during playback.
     */
    fun rotationImage(img: CircleImageView) {
        (img.getTag(R.id.tag_avatar_rotation_animator) as? ObjectAnimator)?.cancel()

        img.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val rotationAnimator = ObjectAnimator.ofFloat(img, View.ROTATION, 0f, 360f).apply {
            duration = Constants.TIME_ROTATE
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    img.setLayerType(View.LAYER_TYPE_NONE, null)
                }

                override fun onAnimationCancel(animation: Animator) {
                    img.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            })
        }
        img.setTag(R.id.tag_avatar_rotation_animator, rotationAnimator)
        rotationAnimator.start()
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
