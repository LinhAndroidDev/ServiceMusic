package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.serviceandroid.databinding.ItemPagerNewReleaseBinding
import com.example.serviceandroid.databinding.PagerNewReleaseBinding

class PagerNationalAdapter : Adapter<PagerNationalAdapter.ViewHolder>() {
    inner class ViewHolder(val v: PagerNewReleaseBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PagerNationalAdapter.ViewHolder = ViewHolder(
        PagerNewReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: PagerNationalAdapter.ViewHolder, position: Int) {
        holder.v.rcvPagerRelease.adapter = PagerNewReleaseAdapter()
    }

    override fun getItemCount(): Int = 3
}

class PagerNewReleaseAdapter : Adapter<PagerNewReleaseAdapter.ViewHolder>() {

    inner class ViewHolder(val v: ItemPagerNewReleaseBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PagerNewReleaseAdapter.ViewHolder = ViewHolder(
        ItemPagerNewReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: PagerNewReleaseAdapter.ViewHolder, position: Int) {

    }

    override fun getItemCount(): Int = 3

}