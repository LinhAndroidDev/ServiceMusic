package com.example.serviceandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemAdvertisementBinding
import com.example.serviceandroid.model.Advertisement

class AdvertisementAdapter(private val context: Context) : Adapter<AdvertisementAdapter.ViewHolder>() {
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
        Glide.with(context)
            .load(advertisements[position].image)
            .error(R.drawable.ic_launcher_background)
            .placeholder(R.mipmap.ic_launcher)
            .centerCrop().into(holder.v.img)

        holder.v.update.text = advertisements[position].update
        holder.v.detail.text = advertisements[position].detail
    }

    override fun getItemCount(): Int = advertisements.size
}