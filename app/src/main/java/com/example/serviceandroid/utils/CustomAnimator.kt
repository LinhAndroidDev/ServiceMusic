package com.example.serviceandroid.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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

    /**
     * Transport control tap feedback: quick press-in, then a soft bounce back.
     * [onClick] runs immediately so playback feels responsive.
     */
    fun animateTransportButton(button: View, onClick: () -> Unit) {
        (button.getTag(R.id.tag_transport_button_animator) as? Animator)?.cancel()
        onClick()

        button.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val pressIn = ObjectAnimator.ofPropertyValuesHolder(
            button,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, TRANSPORT_BUTTON_PRESS_SCALE),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, TRANSPORT_BUTTON_PRESS_SCALE),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.82f),
        ).apply {
            duration = TRANSPORT_BUTTON_PRESS_IN_MS
            interpolator = transportButtonPressInterpolator
        }

        val bounceOut = ObjectAnimator.ofPropertyValuesHolder(
            button,
            PropertyValuesHolder.ofFloat(View.SCALE_X, TRANSPORT_BUTTON_PRESS_SCALE, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, TRANSPORT_BUTTON_PRESS_SCALE, 1f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.82f, 1f),
        ).apply {
            duration = TRANSPORT_BUTTON_BOUNCE_MS
            interpolator = OvershootInterpolator(TRANSPORT_BUTTON_OVERSHOOT_TENSION)
        }

        AnimatorSet().apply {
            playSequentially(pressIn, bounceOut)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    resetTransportButtonTransform(button)
                }

                override fun onAnimationCancel(animation: Animator) {
                    resetTransportButtonTransform(button)
                }
            })
            button.setTag(R.id.tag_transport_button_animator, this)
            start()
        }
    }

    private fun resetTransportButtonTransform(button: View) {
        button.scaleX = 1f
        button.scaleY = 1f
        button.alpha = 1f
        button.setLayerType(View.LAYER_TYPE_NONE, null)
        button.setTag(R.id.tag_transport_button_animator, null)
    }

    private val transportButtonPressInterpolator = FastOutSlowInInterpolator()

    private const val TRANSPORT_BUTTON_PRESS_SCALE = 0.86f
    private const val TRANSPORT_BUTTON_PRESS_IN_MS = 90L
    private const val TRANSPORT_BUTTON_BOUNCE_MS = 320L
    private const val TRANSPORT_BUTTON_OVERSHOOT_TENSION = 2.8f
}
