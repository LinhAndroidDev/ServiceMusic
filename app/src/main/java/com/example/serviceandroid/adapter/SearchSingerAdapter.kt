package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemSearchSingerBinding
import com.example.serviceandroid.model.Singer

class SearchSingerAdapter : RecyclerView.Adapter<SearchSingerAdapter.SingerVH>() {

    var onClickSinger: ((Singer) -> Unit)? = null

    private val items = mutableListOf<Singer>()

    fun submit(singers: List<Singer>) {
        items.clear()
        items.addAll(singers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingerVH {
        val binding = ItemSearchSingerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SingerVH(binding)
    }

    override fun onBindViewHolder(holder: SingerVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SingerVH(
        private val binding: ItemSearchSingerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(singer: Singer) {
            binding.imgSingerAvatar.load(singer.avatarUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_circle)
                error(R.drawable.ic_circle)
            }
            binding.tvSingerName.text = singer.name
            binding.root.setOnClickListener { onClickSinger?.invoke(singer) }
        }
    }
}
