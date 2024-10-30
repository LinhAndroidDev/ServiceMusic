package com.example.serviceandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.serviceandroid.R


class CustomArrayAdapter(context: Context?, resource: Int, objects: List<String?>?) :
    ArrayAdapter<String?>(context!!, resource, objects!!) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.layout_auto_text, parent, false)
        }
        val textView = convertView!!.findViewById<TextView>(R.id.textAuto)
        textView.text = getItem(position)
        return convertView
    }
}