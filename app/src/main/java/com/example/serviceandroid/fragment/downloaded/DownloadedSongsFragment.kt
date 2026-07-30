package com.example.serviceandroid.fragment.downloaded

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.databinding.FragmentDownloadedSongsBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class DownloadedSongsFragment : BaseFragment<FragmentDownloadedSongsBinding>() {
    private val viewModel by viewModels<DownloadedSongsViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private lateinit var adapter: PagerNewReleaseAdapter

    override fun initView() {
        initListSong()
    }

    override fun onClickView() {
        binding.backDownloadedSongs.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun initListSong() {
        adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            onClickItem = { songId ->
                val songs = viewModel.songs.value
                val song = songs.find { it.id == songId }
                if (song != null) {
                    playbackViewModel.setPlaybackQueue(songs)
                    playbackViewModel.playSong(requireContext(), song)
                    MusicPlayerLauncher.open(this@DownloadedSongsFragment, songId, preservePlayback = true)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.songs.collect { songs ->
                    binding.numberSong.text = getString(R.string.downloaded_songs_count, songs.size)
                    binding.notFoundSong.isVisible = songs.isEmpty()
                    adapter.items = ArrayList(songs)
                    binding.rcvDownloadedSongs.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentDownloadedSongsBinding.inflate(inflater)
}
