package com.example.serviceandroid.fragment.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.LibraryAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentLibraryBinding
import com.example.serviceandroid.fragment.favourite_song.FragmentFavouriteSongViewModel
import com.example.serviceandroid.model.Library
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    val viewModel by viewModels<FragmentFavouriteSongViewModel>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Thư viện"
        initLibrary()
    }

    private fun initLibrary() {
        lifecycleScope.launch {
            viewModel.getAll()
            viewModel.songs.collect { songs ->
                var numberSong = 0
                songs?.let { numberSong = it.size }
                val librarys = arrayListOf(
                    Library(R.drawable.favourite, "Bài hát yêu thích", numberSong, R.color.bg_blue),
                    Library(R.drawable.ic_download, "Đã tải", 0, R.color.bg_purple),
                    Library(R.drawable.ic_artist, "Nghệ sĩ", 0, R.color.bg_orange),
                    Library(R.drawable.ic_upload, "Upload", 0, R.color.yellow_dark),
                    Library(R.drawable.ic_mv, "MV", 0, R.color.bg_purple),
                )
                val libraryAdapter = LibraryAdapter(requireActivity())
                libraryAdapter.items = librarys
                libraryAdapter.onClickItem = {
                    if(it == 0) {
                        val navHostFragment =
                            requireActivity().supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
                        val navController = navHostFragment.navController
                        navController.navigate(R.id.favouriteSongFragment)
                    }
                }
                binding.rcvLibrary.adapter = libraryAdapter
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentLibraryBinding.inflate(inflater)

}

