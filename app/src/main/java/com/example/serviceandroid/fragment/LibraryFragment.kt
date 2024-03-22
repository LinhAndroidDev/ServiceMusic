package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentLibraryBinding

class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Thư viện"
    }
    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentLibraryBinding.inflate(inflater)

}

