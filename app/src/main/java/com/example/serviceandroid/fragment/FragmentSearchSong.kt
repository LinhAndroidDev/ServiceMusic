package com.example.serviceandroid.fragment

import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentSearchSongBinding

@Suppress("DEPRECATION")
class FragmentSearchSong : BaseFragment<FragmentSearchSongBinding>() {

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongBinding.inflate(inflater)

    override fun initView() {
        binding.searchSong.showActionSearch()

        ViewCompat.setOnApplyWindowInsetsListener(binding.microSearch) { v, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            v.translationY = if (imeHeight > 0) {
                binding.microSearch.isVisible = true
                -imeHeight.toFloat()
            } else {
                binding.microSearch.isVisible = false
                0f
            }

            insets
        }
    }

    override fun onClickView() {
        binding.backSearch.setOnClickListener { activity?.onBackPressed() }
    }
}