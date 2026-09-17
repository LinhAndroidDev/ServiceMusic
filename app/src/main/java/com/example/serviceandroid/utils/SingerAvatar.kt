package com.example.serviceandroid.utils

import android.widget.ImageView
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.example.serviceandroid.R

fun ImageView.loadSingerAvatar(url: String?) {
    val targetSize = avatarRequestSize()
    load(url?.takeIf { it.isNotBlank() }) {
        crossfade(true)
        size(targetSize)
        scale(Scale.FILL)
        transformations(CircleCropTransformation())
        placeholder(R.drawable.bg_grey_circle)
        error(R.drawable.bg_grey_circle)
    }
}

private fun ImageView.avatarRequestSize(): Int {
    val laidOut = maxOf(width, height)
    if (laidOut > 0) return laidOut
    val spec = maxOf(layoutParams?.width ?: 0, layoutParams?.height ?: 0)
    if (spec > 0) return spec
    return resources.getDimensionPixelSize(R.dimen.singer_avatar_list)
}
