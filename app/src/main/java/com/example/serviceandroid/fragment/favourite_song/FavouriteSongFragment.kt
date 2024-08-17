package com.example.serviceandroid.fragment.favourite_song

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.BottomSheetSongArrangement
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.database.repository.ArrangeMusic
import com.example.serviceandroid.databinding.FragmentFavouriteSongBinding
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
@AndroidEntryPoint
class FavouriteSongFragment : BaseFragment<FragmentFavouriteSongBinding>() {
    private val viewModel: FragmentFavouriteSongViewModel by viewModels()
    private lateinit var adapterFavouriteSong: PagerNewReleaseAdapter

    override fun initView() {
        resetTextTypeArrange()
        initListSong()
    }

    private fun resetTextTypeArrange() {
        viewModel.shared.getTypeArrangement().let { type ->
            when(type) {
                ArrangeMusic.NEWEST -> {
                    binding.tvTypeArrange.text = "Mới nhất"
                }

                ArrangeMusic.OLDEST -> {
                    binding.tvTypeArrange.text = "Cũ nhất"
                }

                ArrangeMusic.BY_NAME_SONG -> {
                    binding.tvTypeArrange.text = "Tên bài hát (A-Z)"
                }

                ArrangeMusic.BY_NAME_SINGLE -> {
                    binding.tvTypeArrange.text = "Tên nghệ sĩ (A-Z)"
                }
            }
        }
    }

    override fun onClickView() {
        binding.backFavouriteSong.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.understood.setOnClickListener {
            binding.filterGuide.isVisible = false
        }

        binding.arrangement.setOnClickListener {
            val bottomSheet = BottomSheetSongArrangement()
            bottomSheet.onClickChangeState = {
                viewModel.getAll()
                resetTextTypeArrange()
            }
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun initListSong() {
        adapterFavouriteSong = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            isFavourite = true
            onClickUnFavourite = { index ->
                DialogConfirm().apply {
                    title = adapterFavouriteSong.items[index].title
                    onClickRemove = {
                        viewModel.deleteSongById(adapterFavouriteSong.items[index].idSong) {
                            notifyDataSetChanged()
                        }
                    }
                }.show(requireActivity().supportFragmentManager, "")
            }
            onClickMoreOption = { song ->
                val dialog = BottomSheetOptionMusic()
                val bundle = Bundle()
                bundle.putParcelable(Constant.KEY_SONG, song)
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "")
            }
        }

        viewModel.getAll()
        lifecycleScope.launch {
            viewModel.songs.collect { songs ->
                songs?.let {
                    binding.numberSong.text = "${songs.size} bài hát . Đã lưu vào thư viện"
                    binding.notFoundSong.visibility = if(songs.size > 0) View.GONE else View.VISIBLE
                    adapterFavouriteSong.items = songs
                    binding.rcvFavouriteSong.adapter = adapterFavouriteSong
                }
            }
        }

        lifecycleScope.launch {
            delay(3000)
            withContext(Dispatchers.Main) {
                if(!binding.notFoundSong.isVisible) {
                    binding.filterGuide.isVisible = true
                }
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
            = FragmentFavouriteSongBinding.inflate(inflater)
}