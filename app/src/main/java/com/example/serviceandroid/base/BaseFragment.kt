package com.example.serviceandroid.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.serviceandroid.MainActivity

abstract class BaseFragment<VB: ViewBinding>: Fragment(), CoreInterface.AndroidView {
    protected lateinit var binding: VB
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getFragmentBinding(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
         * Visible Bottom Navigation Bar When Into Fragment Home
         */
        (activity as MainActivity).visibleBottomBar()
        initView()
        onClickView()
    }

    protected abstract fun getFragmentBinding(inflater: LayoutInflater): VB
}