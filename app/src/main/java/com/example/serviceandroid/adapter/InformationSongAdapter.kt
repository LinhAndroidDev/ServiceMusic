package com.example.serviceandroid.adapter

import android.annotation.SuppressLint
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemInfomationSongBinding
import com.example.serviceandroid.model.Song

class InformationSongAdapter : BaseAdapter<Song, ItemInfomationSongBinding>() {
    var onClickView: (() -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_infomation_song

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemInfomationSongBinding>,
        position: Int
    ) {
        val item = items[position]
        holder.v.apply {
            title.text = item.title
            nameSingle.text = "Ca sĩ: ${item.nameSinger}"
        }

        holder.itemView.setOnClickListener {
            onClickView?.invoke()
        }
    }
}