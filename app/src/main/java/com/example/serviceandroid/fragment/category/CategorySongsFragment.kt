package com.example.serviceandroid.fragment.category

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.databinding.FragmentCategorySongsBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class CategorySongsFragment : BaseFragment<FragmentCategorySongsBinding>() {
    private val viewModel by viewModels<CategorySongsViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private lateinit var adapter: PagerNewReleaseAdapter

    override fun initView() {
        binding.categoryTitle.text = viewModel.title
        initListSong()
    }

    override fun onClickView() {
        binding.backCategorySongs.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initListSong() {
        adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            onClickItem = { songId ->
                val songs = viewModel.uiState.value.songs
                if (playbackViewModel.playFromVisibleList(requireContext(), songs, songId)) {
                    MusicPlayerLauncher.open(
                        this@CategorySongsFragment,
                        songId,
                        preservePlayback = true,
                    )
                }
            }
            onClickMoreOption = { song ->
                val dialog = BottomSheetOptionMusic()
                dialog.removeFavourite = null
                val bundle = Bundle()
                bundle.putParcelable(Constant.KEY_SONG, song)
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "")
            }
        }
        binding.rcvCategorySongs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.numberSong.text =
                        getString(R.string.category_songs_count, state.songs.size)
                    binding.categoryProgress.isVisible = state.isLoading
                    binding.rcvCategorySongs.isVisible = !state.isLoading && state.songs.isNotEmpty()
                    binding.notFoundSong.isVisible = !state.isLoading && state.songs.isEmpty()
                    adapter.items = ArrayList(state.songs)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentCategorySongsBinding.inflate(inflater)
}
