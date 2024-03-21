package com.example.serviceandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import coil.load
import com.example.serviceandroid.databinding.ItemAdvertisementBinding
import com.example.serviceandroid.model.Advertisement

class AdvertisementAdapter : Adapter<AdvertisementAdapter.ViewHolder>() {
    var advertisements = arrayListOf<Advertisement>()

    inner class ViewHolder(val v: ItemAdvertisementBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdvertisementAdapter.ViewHolder {
        val v = ItemAdvertisementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: AdvertisementAdapter.ViewHolder, position: Int) {
        holder.v.img.load(advertisements[position].image) {
            crossfade(true)
        }

        holder.v.update.text = advertisements[position].update
        holder.v.detail.text = advertisements[position].detail
    }

    override fun getItemCount(): Int = advertisements.size
}