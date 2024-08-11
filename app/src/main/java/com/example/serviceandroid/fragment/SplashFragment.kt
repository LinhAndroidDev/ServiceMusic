package com.example.serviceandroid.fragment

import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentSplashBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    override fun initView() {
        lifecycleScope.launch {
            delay(2000L)
            withContext(Dispatchers.Main) {
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    override fun onClickView() {

    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentSplashBinding.inflate(inflater)
}