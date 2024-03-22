package com.example.serviceandroid.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB: ViewBinding>: Fragment() {
    protected lateinit var binding: VB
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getFragmentBinding(layoutInflater)
        return binding.root
    }

    fun changeColorStatusBar(color: Int) {
        requireActivity().window.statusBarColor = color
    }

    protected abstract fun getFragmentBinding(inflater: LayoutInflater): VB
}