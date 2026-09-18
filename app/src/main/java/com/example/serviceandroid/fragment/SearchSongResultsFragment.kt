package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.SearchSongAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.databinding.FragmentSearchSongResultsBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchSongResultsFragment : BaseFragment<FragmentSearchSongResultsBinding>() {

    private val searchViewModel by viewModels<SearchViewModel>({ requireParentFragment() })
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private var songAdapter: SearchSongAdapter? = null

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongResultsBinding.inflate(inflater)

    override fun initView() {
        binding.tvSearchPageEmpty.setText(R.string.search_songs_empty)
        val adapter = SearchSongAdapter().also { created ->
            created.onClickSong = { song ->
                searchViewModel.recordCurrentQuery()
                if (playbackViewModel.playFromVisibleList(
                        requireContext(),
                        searchViewModel.uiState.value.songs,
                        song.id,
                    )
                ) {
                    MusicPlayerLauncher.open(this, song.id, preservePlayback = true)
                }
            }
            created.onClickSongMore = { song -> showMoreOptions(song) }
            songAdapter = created
        }
        binding.rcvSearchPage.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvSearchPage.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchViewModel.uiState.collect { state ->
                    adapter.submit(state.songs)
                    binding.rcvSearchPage.isVisible = state.songs.isNotEmpty()
                    binding.tvSearchPageEmpty.isVisible = state.songs.isEmpty()
                }
            }
        }
    }

    override fun onClickView() = Unit

    private fun showMoreOptions(song: Song) {
        val dialog = BottomSheetOptionMusic()
        val bundle = Bundle()
        bundle.putParcelable(Constant.KEY_SONG, song)
        dialog.arguments = bundle
        dialog.show(parentFragmentManager, "")
    }
}
