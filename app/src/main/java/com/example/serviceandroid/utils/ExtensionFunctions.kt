package com.example.serviceandroid.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.example.serviceandroid.R
import com.example.serviceandroid.database.SongEntity
import com.example.serviceandroid.model.Song
import com.google.android.material.snackbar.Snackbar

object ExtensionFunctions {
    fun <T : ViewDataBinding?> AppCompatActivity.getViewBinding(@LayoutRes resId: Int): T {
        return DataBindingUtil.setContentView<T>(this, resId)
    }

    fun NestedScrollView.isViewVisible(view: View): Boolean {
        val scrollBounds = Rect()
        this.getDrawingRect(scrollBounds)
        val top = view.y
//        val bottom = view.height + top
        return scrollBounds.top > top
    }

    fun gradientTextColor(tv: TextView) {
        tv.let {
            val paint = it.paint
            val width = paint.measureText(it.text.toString())
            val textShader: Shader = LinearGradient(
                0f,
                0f,
                width,
                it.textSize,
                intArrayOf(Color.CYAN, Color.MAGENTA, Color.YELLOW),
                null,
                Shader.TileMode.CLAMP
            )
            it.paint.shader = textShader
        }
    }

    fun View.addCircleRipple() = with(TypedValue()) {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
        foreground = ContextCompat.getDrawable(context, resourceId)
    }

    fun View.snackBar(message: String) {
        Snackbar.make(this, message, Snackbar.LENGTH_SHORT)
            .also { snackbar ->
                snackbar.setAction("OK") {
                    snackbar.dismiss()
                }
            }
            .show()
    }

    @SuppressLint("Range")
    fun ContentResolver.getFileName(uri: Uri): String {
        var name = ""
        val cursor = query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        return name
    }

    fun SongEntity.toSong(): Song {
        return Song(
            this.idSong,
            this.title,
            this.nameSinger,
            this.avatar,
            this.sing,
            this.time,
            this.type
        )
    }

    fun View.showKeyboard() {
        this.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun Fragment.setColorTint(imageView: ImageView, color: Int) {
        imageView.setColorFilter(ContextCompat.getColor(requireActivity(), color), PorterDuff.Mode.SRC_IN)
    }
}