package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import coil.load
import com.example.serviceandroid.databinding.ItemAdvertisementBinding
import com.example.serviceandroid.model.Advertisement

class AdvertisementAdapter : Adapter<AdvertisementAdapter.ViewHolder>() {
    var advertisements = mutableListOf<Advertisement>()

    class ViewHolder(private val v: ItemAdvertisementBinding) : RecyclerView.ViewHolder(v.root) {
        fun bindData(advertisement: Advertisement) {
            v.apply {
                img.load(advertisement.image) {
                    crossfade(true)
                }
                update.text = advertisement.update
                detail.text = advertisement.detail
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdvertisementAdapter.ViewHolder {
        val v = ItemAdvertisementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: AdvertisementAdapter.ViewHolder, position: Int) {
        val advertisement = advertisements[position]
        holder.bindData(advertisement)
    }

    override fun getItemCount(): Int = advertisements.size
}