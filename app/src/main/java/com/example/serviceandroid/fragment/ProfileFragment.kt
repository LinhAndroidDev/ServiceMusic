package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentProfileBinding

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Cá nhân"
    }
    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentProfileBinding.inflate(inflater)
}