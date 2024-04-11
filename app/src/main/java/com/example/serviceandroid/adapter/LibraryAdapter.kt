package com.example.serviceandroid.adapter

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemLibraryBinding
import com.example.serviceandroid.model.Library

class LibraryAdapter(private val context: Context) : BaseAdapter<Library, ItemLibraryBinding>() {
    var onClickItem: ((Int) -> Unit)? = null
    override fun getLayout(): Int = R.layout.item_library

    override fun onBindViewHolder(holder: BaseViewHolder<ItemLibraryBinding>, position: Int) {
        val item = items[position]
        holder.v.apply {
            viewMargin.isVisible = position == 0
            iconLibrary.setImageResource(item.icon)
            iconLibrary.setColorFilter(context.getColor(item.colorIcon))
            tvNumber.visibility = if (item.number == 0) {
                View.INVISIBLE
            } else {
                tvNumber.text = item.number.toString()
                View.VISIBLE
            }
            tvNameLibrary.text = item.nameLibrary
        }

        holder.itemView.setOnClickListener {
            onClickItem?.invoke(position)
        }
    }
}