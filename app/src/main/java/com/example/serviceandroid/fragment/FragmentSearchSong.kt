package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serviceandroid.adapter.SearchResultsAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.databinding.FragmentSearchSongBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentSearchSong : BaseFragment<FragmentSearchSongBinding>() {

    private val searchViewModel by viewModels<SearchViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private var searchJob: Job? = null
    private var searchAdapter: SearchResultsAdapter? = null

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongBinding.inflate(inflater)

    override fun initView() {
        binding.rcvSearchResults.layoutManager = LinearLayoutManager(requireContext())

        ViewCompat.setOnApplyWindowInsetsListener(binding.microSearch) { v, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            v.translationY = if (imeHeight > 0) {
                binding.microSearch.isVisible = true
                -imeHeight.toFloat()
            } else {
                binding.microSearch.isVisible = false
                0f
            }

            insets
        }

        binding.searchSong.onQueryChanged = { query ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(350)
                if (query.isBlank()) {
                    searchViewModel.clearResults()
                } else {
                    searchViewModel.search(query)
                }
            }
            if (query.isBlank()) {
                bindSearchChrome(
                    showSuggestions = true,
                    showResults = false,
                    showEmpty = false,
                    showLoading = false,
                )
            } else if (!searchViewModel.uiState.value.hasResults ||
                searchViewModel.uiState.value.query != query.trim()
            ) {
                bindSearchChrome(
                    showSuggestions = false,
                    showResults = false,
                    showEmpty = false,
                    showLoading = true,
                )
            }
        }

        val restored = searchViewModel.uiState.value
        if (restored.query.isNotBlank()) {
            binding.searchSong.setQuery(restored.query, notify = false)
            applyState(restored)
        } else {
            binding.searchSong.showActionSearch()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchViewModel.uiState.collect { state ->
                    applyState(state)
                }
            }
        }
    }

    private fun applyState(state: SearchUiState) {
        val querying = state.query.isNotBlank() || binding.searchSong.queryText().isNotBlank()
        when {
            !querying -> bindSearchChrome(
                showSuggestions = true,
                showResults = false,
                showEmpty = false,
                showLoading = false,
            )
            state.isSearching -> bindSearchChrome(
                showSuggestions = false,
                showResults = state.hasResults,
                showEmpty = false,
                showLoading = !state.hasResults,
            )
            state.hasResults -> {
                ensureSearchAdapter().submit(state.songs, state.singers)
                if (binding.rcvSearchResults.adapter !== searchAdapter) {
                    binding.rcvSearchResults.adapter = searchAdapter
                }
                bindSearchChrome(
                    showSuggestions = false,
                    showResults = true,
                    showEmpty = false,
                    showLoading = false,
                )
            }
            else -> bindSearchChrome(
                showSuggestions = false,
                showResults = false,
                showEmpty = true,
                showLoading = false,
            )
        }
    }

    private fun bindSearchChrome(
        showSuggestions: Boolean,
        showResults: Boolean,
        showEmpty: Boolean,
        showLoading: Boolean,
    ) {
        binding.contentView.isVisible = showSuggestions
        binding.rcvSearchResults.isVisible = showResults
        binding.searchEmptyState.isVisible = showEmpty
        binding.searchProgress.isVisible = showLoading
    }

    private fun ensureSearchAdapter(): SearchResultsAdapter {
        searchAdapter?.let { return it }
        val adapter = SearchResultsAdapter()
        adapter.onClickSong = { song ->
            val songs = searchViewModel.uiState.value.songs
            playbackViewModel.setPlaybackQueue(songs)
            playbackViewModel.playSong(requireContext(), song)
            MusicPlayerLauncher.open(this, song.id, preservePlayback = true)
        }
        adapter.onClickSongMore = { song -> showMoreOptions(song) }
        adapter.onClickSinger = { singer ->
            val action = FragmentSearchSongDirections
                .actionFragmentSearchSongToSingerDetailFragment(singer.id)
            findNavController().navigate(action)
        }
        searchAdapter = adapter
        return adapter
    }

    private fun showMoreOptions(song: Song) {
        val dialog = BottomSheetOptionMusic()
        val bundle = Bundle()
        bundle.putParcelable(Constant.KEY_SONG, song)
        dialog.arguments = bundle
        dialog.show(parentFragmentManager, "")
    }

    override fun onClickView() {
        binding.backSearch.setOnClickListener { activity?.onBackPressed() }
    }
}
