package com.example.serviceandroid.fragment.favourite_song

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentFavouriteSongBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class FavouriteSongFragment : BaseFragment<FragmentFavouriteSongBinding>() {
    private val viewModel: FragmentFavouriteSongViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClickView()
        initListSong()
    }

    private fun onClickView() {
        binding.backFavouriteSong.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun initListSong() {
        lifecycleScope.launch {
            viewModel.getAll()?.let {
                val adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
                    isFavourite = true
                    items = it
                }
                binding.rcvFavouriteSong.adapter = adapter
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
            = FragmentFavouriteSongBinding.inflate(inflater)
}