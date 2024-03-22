package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentRadioBinding

class RadioFragment : BaseFragment<FragmentRadioBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Radio"
    }
    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentRadioBinding.inflate(inflater)
}