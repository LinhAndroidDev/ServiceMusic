package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import coil.load
import com.example.serviceandroid.databinding.ItemAdvertisementBinding
import com.example.serviceandroid.model.Advertisement

class AdvertisementAdapter(
    val itemWidthPx: Int,
) : Adapter<AdvertisementAdapter.ViewHolder>() {

    var advertisements: List<Advertisement> = emptyList()
        private set

    val realItemCount: Int
        get() = advertisements.size

    fun submitAdvertisements(ads: List<Advertisement>) {
        advertisements = ads
        notifyDataSetChanged()
    }

    fun getInfiniteStartPosition(): Int = getInfiniteMiddleOffset()

    fun getInfiniteMiddleOffset(): Int {
        if (advertisements.size <= 1) return 0
        return advertisements.size * (INFINITE_SCROLL_MULTIPLIER / 2)
    }

    fun toRealIndex(adapterPosition: Int): Int {
        val count = advertisements.size
        if (count <= 0) return 0
        return ((adapterPosition % count) + count) % count
    }

    inner class ViewHolder(private val binding: ItemAdvertisementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(advertisement: Advertisement) {
            binding.apply {
                img.load(advertisement.image) {
                    crossfade(true)
                }
                update.text = advertisement.update
                detail.text = advertisement.detail
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdvertisementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        binding.root.layoutParams = RecyclerView.LayoutParams(
            itemWidthPx,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (advertisements.isEmpty()) return
        holder.bindData(advertisements[toRealIndex(position)])
    }

    override fun getItemCount(): Int {
        if (advertisements.size <= 1) return advertisements.size
        return advertisements.size * INFINITE_SCROLL_MULTIPLIER
    }

    companion object {
        private const val INFINITE_SCROLL_MULTIPLIER = 1_000
    }
}
