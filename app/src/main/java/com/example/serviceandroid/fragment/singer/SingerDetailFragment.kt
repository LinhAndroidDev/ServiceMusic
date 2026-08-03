package com.example.serviceandroid.fragment.singer

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.databinding.FragmentSingerDetailBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class SingerDetailFragment : BaseFragment<FragmentSingerDetailBinding>() {

    private val args by navArgs<SingerDetailFragmentArgs>()
    private val viewModel by viewModels<SingerDetailViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private var songsAdapter: PagerNewReleaseAdapter? = null

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSingerDetailBinding.inflate(inflater)

    override fun initView() {
        ensureSongsAdapter()
        viewModel.load(args.singerId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressLoading.isVisible = state.isLoading
                    val singer = state.singer
                    if (singer != null) {
                        binding.tvSingerName.text = singer.name
                        binding.imgSingerAvatar.load(singer.avatarUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_circle)
                            error(R.drawable.ic_circle)
                        }
                        val description = singer.description.trim()
                        binding.tvSingerDescription.isVisible = description.isNotEmpty()
                        binding.tvSingerDescription.text =
                            description.ifEmpty { getString(R.string.singer_bio_empty) }
                    } else if (!state.isLoading && state.error) {
                        binding.tvSingerName.setText(R.string.singer_info_empty)
                        binding.tvSingerDescription.isVisible = false
                    }

                    binding.tvSongCount.text =
                        getString(R.string.singer_detail_songs_count, state.songs.size)
                    binding.tvEmptySongs.isVisible =
                        !state.isLoading && state.songs.isEmpty()
                    binding.rcvSingerSongs.isVisible = state.songs.isNotEmpty()

                    songsAdapter?.let { adapter ->
                        adapter.items = ArrayList(state.songs)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun ensureSongsAdapter() {
        if (songsAdapter != null) return
        val adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            onClickItem = { songId ->
                val songs = viewModel.uiState.value.songs
                val song = songs.find { it.id == songId }
                if (song != null) {
                    playbackViewModel.setPlaybackQueue(songs)
                    playbackViewModel.playSong(requireContext(), song)
                    MusicPlayerLauncher.open(
                        this@SingerDetailFragment,
                        songId,
                        preservePlayback = true,
                    )
                }
            }
            onClickMoreOption = { song ->
                val dialog = BottomSheetOptionMusic()
                val bundle = Bundle()
                bundle.putParcelable(Constant.KEY_SONG, song)
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "")
            }
        }
        songsAdapter = adapter
        binding.rcvSingerSongs.adapter = adapter
    }

    override fun onClickView() {
        binding.backSingerDetail.setOnClickListener {
            activity?.onBackPressed()
        }
    }
}
