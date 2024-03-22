package com.example.serviceandroid.fragment

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentZingChartBinding

class ZingChartFragment : BaseFragment<FragmentZingChartBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "#zingchart"
        binding.header.title.let {
            val paint = it.paint
            val width = paint.measureText(it.text.toString())
            val textShader: Shader = LinearGradient(
                0f,
                0f,
                width,
                it.textSize,
                intArrayOf(Color.CYAN, Color.MAGENTA, Color.YELLOW),
                null,
                Shader.TileMode.CLAMP
            )
            it.paint.shader = textShader
        }
    }
    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentZingChartBinding.inflate(inflater)

}