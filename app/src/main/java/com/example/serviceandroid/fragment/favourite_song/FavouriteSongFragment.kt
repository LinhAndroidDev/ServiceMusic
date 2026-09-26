package com.example.serviceandroid.fragment.favourite_song

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.BottomSheetSongArrangement
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.database.repository.ArrangeMusic
import com.example.serviceandroid.databinding.FragmentFavouriteSongBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
@AndroidEntryPoint
class FavouriteSongFragment : BaseFragment<FragmentFavouriteSongBinding>() {
    private val viewModel: FragmentFavouriteSongViewModel by activityViewModels()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private lateinit var adapterFavouriteSong: PagerNewReleaseAdapter

    override fun initView() {
        resetTextTypeArrange()
        initListSong()
    }

    private fun resetTextTypeArrange() {
        viewModel.getTypeArrangement().let { type ->
            when(type) {
                ArrangeMusic.NEWEST -> {
                    binding.tvTypeArrange.text = getString(R.string.arrange_newest)
                }

                ArrangeMusic.OLDEST -> {
                    binding.tvTypeArrange.text = getString(R.string.arrange_oldest)
                }

                ArrangeMusic.BY_NAME_SONG -> {
                    binding.tvTypeArrange.text = getString(R.string.arrange_by_song_name)
                }

                ArrangeMusic.BY_NAME_SINGLE -> {
                    binding.tvTypeArrange.text = getString(R.string.arrange_by_artist_name)
                }
            }
        }
    }

    override fun onClickView() {
        binding.backFavouriteSong.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.understood.setOnClickListener {
            viewModel.dismissFilterGuide()
            binding.filterGuide.isVisible = false
        }

        binding.arrangement.setOnClickListener {
            val bottomSheet = BottomSheetSongArrangement()
            bottomSheet.onClickChangeState = {
                viewModel.applyCurrentArrangement()
                resetTextTypeArrange()
            }
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun initListSong() {
        adapterFavouriteSong = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            isFavourite = true
            onClickItem = { songId ->
                val songs = adapterFavouriteSong.items.toList()
                if (playbackViewModel.playFromVisibleList(requireContext(), songs, songId)) {
                    MusicPlayerLauncher.open(
                        this@FavouriteSongFragment,
                        songId,
                        preservePlayback = true,
                    )
                }
            }
            onClickUnFavourite = { index ->
                DialogConfirm().apply {
                    title = adapterFavouriteSong.items[index].title
                    onClickRemove = {
                        (activity as? MainActivity)?.requestRemoveFavourite(
                            adapterFavouriteSong.items[index].id
                        ) {
                            notifyDataSetChanged()
                        }
                    }
                }.show(requireActivity().supportFragmentManager, "")
            }
            onClickMoreOption = { song ->
                val dialog = BottomSheetOptionMusic()
                dialog.removeFavourite = {
                    DialogConfirm().apply {
                        title = song.title
                        onClickRemove = {
                            (activity as? MainActivity)?.requestRemoveFavourite(song.id) {
                                Toast.makeText(
                                    requireActivity(),
                                    getString(R.string.toast_removed_favourite),
                                    Toast.LENGTH_SHORT
                                ).show()
                                notifyDataSetChanged()
                            }
                        }
                    }.show(requireActivity().supportFragmentManager, "")
                }
                val bundle = Bundle()
                bundle.putParcelable(Constant.KEY_SONG, song)
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "")
            }
        }

        lifecycleScope.launch {
            viewModel.songs.collect { songs ->
                songs?.let {
                    binding.numberSong.text =
                        getString(R.string.favourite_songs_count, songs.size)
                    binding.notFoundSong.visibility = if(songs.size > 0) View.GONE else View.VISIBLE
                    adapterFavouriteSong.items = songs
                    binding.rcvFavouriteSong.adapter = adapterFavouriteSong
                }
            }
        }

        lifecycleScope.launch {
            if (viewModel.isFilterGuideDismissed()) return@launch
            delay(3000)
            withContext(Dispatchers.Main) {
                if (!isAdded || viewModel.isFilterGuideDismissed()) return@withContext
                if (!binding.notFoundSong.isVisible) {
                    binding.filterGuide.isVisible = true
                }
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
            = FragmentFavouriteSongBinding.inflate(inflater)
}