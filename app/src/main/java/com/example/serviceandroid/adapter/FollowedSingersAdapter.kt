package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.databinding.ItemAddArtistFooterBinding
import com.example.serviceandroid.databinding.ItemSearchSingerBinding
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.utils.loadSingerAvatar

class FollowedSingersAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onClickSinger: ((Singer) -> Unit)? = null
    var onClickAdd: (() -> Unit)? = null

    private val items = mutableListOf<Singer>()

    fun submit(singers: List<Singer>) {
        items.clear()
        items.addAll(singers)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (position == items.size) TYPE_FOOTER else TYPE_SINGER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FOOTER) {
            FooterVH(ItemAddArtistFooterBinding.inflate(inflater, parent, false))
        } else {
            SingerVH(ItemSearchSingerBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SingerVH -> holder.bind(items[position])
            is FooterVH -> holder.binding.root.setOnClickListener { onClickAdd?.invoke() }
        }
    }

    override fun getItemCount(): Int = items.size + 1

    private inner class SingerVH(
        private val binding: ItemSearchSingerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(singer: Singer) {
            binding.imgSingerAvatar.loadSingerAvatar(singer.avatarUrl)
            binding.tvSingerName.text = singer.name
            binding.root.setOnClickListener { onClickSinger?.invoke(singer) }
        }
    }

    private class FooterVH(val binding: ItemAddArtistFooterBinding) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_SINGER = 0
        const val TYPE_FOOTER = 1
    }
}
