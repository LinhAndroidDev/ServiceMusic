package com.example.serviceandroid.adapter

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemUpdateAccountBinding
import com.example.serviceandroid.model.UpdateAccount
import com.example.serviceandroid.utils.Convert

class UpdateAccountAdapter(private val context: Context) : BaseAdapter<UpdateAccount, ItemUpdateAccountBinding>() {
    override fun getLayout(): Int = R.layout.item_update_account

    override fun onBindViewHolder(holder: BaseViewHolder<ItemUpdateAccountBinding>, position: Int) {
        val layoutParams = holder.v.root.layoutParams
        layoutParams.width = (Convert.getWidthDevice(context) * 0.8).toInt()
        holder.v.root.layoutParams = layoutParams

        val updateAccount = items[position]
        holder.v.apply {
            textZingMP3.setTextColor(context.getColor(updateAccount.color))
            textQuality.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, updateAccount.color))
            textQuality.text = updateAccount.nameQuality
            textPrice.text = updateAccount.price
            textNote.text = updateAccount.note
            ability1.text = updateAccount.ability1
            ability2.text = updateAccount.ability2
            ability3.text = updateAccount.ability3
            image1.setImageResource(updateAccount.image1)
            image2.setImageResource(updateAccount.image2)
            image3.setImageResource(updateAccount.image3)
            val colorTint = ColorStateList.valueOf(ContextCompat.getColor(context, updateAccount.color))
            image1.imageTintList  = colorTint
            image2.imageTintList  = colorTint
            image3.imageTintList  = colorTint
            viewContent.setBackgroundResource(updateAccount.backgroundView)
        }
    }
}