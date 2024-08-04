package com.example.serviceandroid.fragment

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentSearchSongBinding

@Suppress("DEPRECATION")
class FragmentSearchSong : BaseFragment<FragmentSearchSongBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchSong.showActionSearch()
        binding.backSearch.setOnClickListener { activity?.onBackPressed() }

        binding.rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private val defaultKeyboardHeightDP = 100
            private val estimatedKeyboardDP = defaultKeyboardHeightDP + 48
            private val rect = Rect()

            override fun onGlobalLayout() {
                activity?.let {
                    binding.rootView.getWindowVisibleDisplayFrame(rect)
                    val heightDiff = binding.rootView.rootView.height - (rect.bottom - rect.top)
                    val isKeyboardShown = heightDiff > estimatedKeyboardDP * resources.displayMetrics.density
                    binding.microSearch.isVisible = isKeyboardShown
                    if (isKeyboardShown) {
                        (activity as MainActivity).hideBottomBar()
                    } else {
                        (activity as MainActivity).visibleBottomBar()
                    }
                }
            }
        })
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongBinding.inflate(inflater)
}