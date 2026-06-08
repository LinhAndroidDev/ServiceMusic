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

    fun submitAdvertisements(ads: List<Advertisement>) {
        advertisements = ads
        notifyDataSetChanged()
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
        holder.bindData(advertisements[position])
    }

    override fun getItemCount(): Int = advertisements.size
}
