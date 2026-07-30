package com.example.serviceandroid.fragment.library

import android.view.LayoutInflater
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.LibraryAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentLibraryBinding
import com.example.serviceandroid.fragment.downloaded.DownloadedSongsViewModel
import com.example.serviceandroid.fragment.favourite_song.FragmentFavouriteSongViewModel
import com.example.serviceandroid.model.Library
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    private val favouriteViewModel by activityViewModels<FragmentFavouriteSongViewModel>()
    private val downloadedViewModel by viewModels<DownloadedSongsViewModel>()

    override fun initView() {
        binding.header.title.text = "Thư viện"
        initLibrary()
    }

    override fun onClickView() {
    }

    private fun initLibrary() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    favouriteViewModel.favouriteCount,
                    downloadedViewModel.count,
                ) { favouriteCount, downloadedCount ->
                    favouriteCount to downloadedCount
                }.collect { (favouriteCount, downloadedCount) ->
                    val librarys = arrayListOf(
                        Library(R.drawable.favourite, "Bài hát yêu thích", favouriteCount, R.color.bg_blue),
                        Library(R.drawable.ic_download, "Đã tải", downloadedCount, R.color.bg_purple),
                        Library(R.drawable.ic_artist, "Nghệ sĩ", 0, R.color.bg_orange),
                        Library(R.drawable.ic_upload, "Upload", 0, R.color.yellow_dark),
                        Library(R.drawable.ic_mv, "MV", 0, R.color.bg_purple),
                    )
                    val libraryAdapter = LibraryAdapter(requireActivity())
                    libraryAdapter.items = librarys
                    libraryAdapter.onClickItem = { index ->
                        val navHostFragment =
                            requireActivity().supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
                        val navController = navHostFragment.navController
                        when (index) {
                            0 -> navController.navigate(R.id.favouriteSongFragment)
                            1 -> navController.navigate(R.id.downloadedSongsFragment)
                        }
                    }
                    binding.rcvLibrary.adapter = libraryAdapter
                }
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentLibraryBinding.inflate(inflater)
}
