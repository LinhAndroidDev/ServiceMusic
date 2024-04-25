package com.example.serviceandroid.fragment.favourite_song

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.FragmentFavouriteSongBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class FavouriteSongFragment : BaseFragment<FragmentFavouriteSongBinding>() {
    private val viewModel: FragmentFavouriteSongViewModel by viewModels()
    private lateinit var adapterFavouriteSong: PagerNewReleaseAdapter
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

    @SuppressLint("NotifyDataSetChanged")
    private fun initListSong() {
        lifecycleScope.launch {
            viewModel.getAll()
            viewModel.songs.collect { songs ->
                songs?.let {
                    binding.notFoundSong.visibility = if(songs.size > 0) View.GONE else View.VISIBLE
                    adapterFavouriteSong = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
                        isFavourite = true
                        items = songs
                        onClickUnFavourite = { index ->
                            DialogConfirm().apply {
                                title = songs[index].title
                                onClickRemove = {
                                    viewModel.deleteSongById(songs[index].id)
                                    notifyDataSetChanged()
                                }
                            }.show(requireActivity().supportFragmentManager, "")
                        }
                    }
                    binding.rcvFavouriteSong.adapter = adapterFavouriteSong
                }
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
            = FragmentFavouriteSongBinding.inflate(inflater)
}