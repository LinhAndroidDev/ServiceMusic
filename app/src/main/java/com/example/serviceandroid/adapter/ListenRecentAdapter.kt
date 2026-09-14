package com.example.serviceandroid.adapter

import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemListenRecentBinding
import com.example.serviceandroid.model.Song

class ListenRecentAdapter : BaseAdapter<Song, ItemListenRecentBinding>() {
    var onClickItem: ((Song) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_listen_recent

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemListenRecentBinding>,
        position: Int,
    ) {
        val song = items[position]
        holder.v.apply {
            recentSongTitle.text = song.title
            recentSongSinger.text = song.nameSinger
            Glide.with(recentSongThumbnail)
                .load(song.thumbnailUrl)
                .placeholder(R.drawable.ic_music)
                .error(R.drawable.ic_music)
                .centerCrop()
                .into(recentSongThumbnail)
        }
        holder.itemView.setOnClickListener { onClickItem?.invoke(song) }
    }
}