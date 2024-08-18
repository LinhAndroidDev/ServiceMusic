package com.example.serviceandroid.adapter

import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemRadioBinding

class RadioAdapter : BaseAdapter<Int, ItemRadioBinding>() {
    override fun getItemCount(): Int = 10

    override fun getLayout(): Int = R.layout.item_radio

    override fun onBindViewHolder(holder: BaseViewHolder<ItemRadioBinding>, position: Int) {

    }
}