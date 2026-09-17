package com.example.serviceandroid.utils

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.example.serviceandroid.R

fun ImageView.loadSongThumbnail(
    url: String?,
    sizePx: Int? = null,
    circle: Boolean = false,
) {
    loadMediaImage(
        url = url,
        sizePx = sizePx,
        circle = circle,
        placeholder = if (circle) R.drawable.bg_grey_circle else R.drawable.bg_grey_corner_5,
        fallbackDimen = if (circle) R.dimen.song_thumbnail_player else R.dimen.song_thumbnail_list,
    )
}

fun ImageView.loadSingerAvatar(url: String?, sizePx: Int? = null) {
    loadMediaImage(
        url = url,
        sizePx = sizePx,
        circle = true,
        placeholder = R.drawable.bg_grey_circle,
        fallbackDimen = R.dimen.singer_avatar_list,
    )
}

suspend fun loadSongThumbnailBitmap(
    context: Context,
    url: String?,
    sizePx: Int = DEFAULT_BITMAP_SIZE,
): Bitmap? {
    if (url.isNullOrBlank()) return null
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(sizePx)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request)
    return (result as? SuccessResult)?.drawable?.toBitmap()
}

private fun ImageView.loadMediaImage(
    url: String?,
    sizePx: Int?,
    circle: Boolean,
    @DrawableRes placeholder: Int,
    fallbackDimen: Int,
) {
    val targetSize = sizePx?.takeIf { it > 0 } ?: imageRequestSize(fallbackDimen)
    load(url?.takeIf { it.isNotBlank() }) {
        crossfade(true)
        size(targetSize)
        scale(Scale.FILL)
        if (circle) transformations(CircleCropTransformation())
        placeholder(placeholder)
        error(placeholder)
    }
}

private fun ImageView.imageRequestSize(fallbackDimen: Int): Int {
    val laidOut = maxOf(width, height)
    if (laidOut > 0) return laidOut
    val spec = maxOf(layoutParams?.width ?: 0, layoutParams?.height ?: 0)
    if (spec > 0) return spec
    return resources.getDimensionPixelSize(fallbackDimen)
}

private const val DEFAULT_BITMAP_SIZE = 256
