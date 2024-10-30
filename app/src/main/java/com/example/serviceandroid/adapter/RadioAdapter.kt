package com.example.serviceandroid.adapter

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemRadioBinding
import com.example.serviceandroid.model.Radio

class RadioAdapter : BaseAdapter<Radio, ItemRadioBinding>() {
    private var indexSelected = 0
    var onItemClick: ((Int) -> Unit)? = null
    override fun getLayout(): Int = R.layout.item_radio

    override fun onBindViewHolder(holder: BaseViewHolder<ItemRadioBinding>, @SuppressLint("RecyclerView") position: Int) {
        val radio = items[position]
        holder.v.viewSelected.isVisible = indexSelected == position
        holder.itemView.setOnClickListener {
            indexSelected = position
            onItemClick?.invoke(position)
            notifyDataSetChanged()
        }
    }
}