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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.SearchPreviewAdapter
import com.example.serviceandroid.adapter.SearchResultsPagerAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.VoiceSearch
import com.example.serviceandroid.data.search.SearchQuery
import com.example.serviceandroid.databinding.FragmentSearchSongBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentSearchSong : BaseFragment<FragmentSearchSongBinding>() {

    private val args by navArgs<FragmentSearchSongArgs>()
    private val searchViewModel by viewModels<SearchViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private var searchJob: Job? = null
    private var tabMediator: TabLayoutMediator? = null
    private var previewAdapter: SearchPreviewAdapter? = null
    private val voiceSearch = VoiceSearch(this) { query ->
        searchJob?.cancel()
        binding.searchSong.setQuery(query, notify = false)
        searchViewModel.commitQuery(query)
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongBinding.inflate(inflater)

    override fun initView() {
        setupResultsPager()
        setupPreviewList()

        binding.microSearch.isVisible = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.microSearch) { v, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.translationY = if (imeHeight > 0) -imeHeight.toFloat() else 0f
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
                bindSearchChrome(SearchChrome.Idle)
            } else if (!searchViewModel.uiState.value.hasResults ||
                searchViewModel.uiState.value.query != query.trim()
            ) {
                bindSearchChrome(SearchChrome.Loading)
            } else {
                bindSearchChrome(SearchChrome.Preview)
            }
        }
        binding.searchSong.onSearchAction = { query ->
            if (query.isNotBlank()) {
                searchJob?.cancel()
                searchViewModel.commitQuery(query)
            }
        }

        val committed = args.committedQuery.trim()
        val restored = searchViewModel.uiState.value
        when {
            committed.isNotBlank() -> {
                binding.searchSong.setQuery(committed, notify = false)
                searchViewModel.commitQuery(committed)
            }
            restored.query.isNotBlank() -> {
                binding.searchSong.setQuery(restored.query, notify = false)
                applyState(restored)
            }
            else -> binding.searchSong.showActionSearch()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchViewModel.uiState.collect { state ->
                    applyState(state)
                }
            }
        }
    }

    private fun setupResultsPager() {
        if (binding.searchResultsPager.adapter != null) return
        binding.searchResultsPager.offscreenPageLimit = 1
        binding.searchResultsPager.adapter = SearchResultsPagerAdapter(this)
        tabMediator = TabLayoutMediator(
            binding.searchResultsTabs,
            binding.searchResultsPager,
        ) { tab, position ->
            tab.setText(
                if (position == SearchResultsPagerAdapter.PAGE_SONGS) {
                    R.string.search_section_songs
                } else {
                    R.string.search_section_artists
                }
            )
        }.also { it.attach() }
    }

    private fun setupPreviewList() {
        val adapter = previewAdapter ?: SearchPreviewAdapter().also { created ->
            created.onClickName = { name ->
                searchJob?.cancel()
                binding.searchSong.setQuery(name, notify = false)
                searchViewModel.commitQuery(name)
            }
            created.onClickSong = { song ->
                searchViewModel.recordCurrentQuery()
                val songs = searchViewModel.uiState.value.songs
                playbackViewModel.setPlaybackQueue(songs)
                playbackViewModel.playSong(requireContext(), song)
                MusicPlayerLauncher.open(this, song.id, preservePlayback = true)
            }
            created.onClickSongMore = { song -> showMoreOptions(song) }
            created.onClickSinger = { singer ->
                searchViewModel.recordCurrentQuery()
                val action = FragmentSearchSongDirections
                    .actionFragmentSearchSongToSingerDetailFragment(singer.id)
                findNavController().navigate(action)
            }
            previewAdapter = created
        }
        if (binding.rcvSearchPreview.adapter !== adapter) {
            binding.rcvSearchPreview.layoutManager = LinearLayoutManager(requireContext())
            binding.rcvSearchPreview.adapter = adapter
        }
    }

    private fun applyState(state: SearchUiState) {
        bindIdleChips(state)
        previewAdapter?.submit(state.relatedNames, state.songs, state.singers)
        val querying = state.query.isNotBlank() || binding.searchSong.queryText().isNotBlank()
        when {
            !querying -> bindSearchChrome(SearchChrome.Idle)
            state.isSearching && !state.hasResults -> bindSearchChrome(SearchChrome.Loading)
            state.mode == SearchResultsMode.COMMITTED && !state.isSearching ->
                bindSearchChrome(SearchChrome.Tabs)
            state.isSearching && state.mode == SearchResultsMode.COMMITTED && state.hasResults ->
                bindSearchChrome(SearchChrome.Tabs)
            else -> bindSearchChrome(SearchChrome.Preview)
        }
    }

    private fun bindSearchChrome(chrome: SearchChrome) {
        binding.contentView.isVisible = chrome == SearchChrome.Idle
        binding.rcvSearchPreview.isVisible = chrome == SearchChrome.Preview
        binding.searchResultsContainer.isVisible = chrome == SearchChrome.Tabs
        binding.searchProgress.isVisible = chrome == SearchChrome.Loading
    }

    private fun bindIdleChips(state: SearchUiState) {
        binding.recentHistorySection.isVisible = state.recentQueries.isNotEmpty()
        bindHistoryChips(state.recentQueries)
        binding.suggestionSection.isVisible = state.suggestions.isNotEmpty()
        bindSuggestionChips(state.suggestions)
    }

    private fun bindHistoryChips(queries: List<SearchQuery>) {
        val group = binding.recentSearchChips
        val current = chipTexts(group)
        val next = queries.map { it.query }
        if (current == next) return
        group.removeAllViews()
        queries.forEach { item ->
            group.addView(
                createChip(
                    text = item.query,
                    showClose = true,
                    onClick = { applyChipQuery(item.query) },
                    onClose = { searchViewModel.deleteRecentQuery(item.normalizedQuery) },
                )
            )
        }
    }

    private fun bindSuggestionChips(suggestions: List<String>) {
        val group = binding.suggestionChips
        if (chipTexts(group) == suggestions) return
        group.removeAllViews()
        suggestions.forEach { text ->
            group.addView(
                createChip(
                    text = text,
                    showClose = false,
                    onClick = { applyChipQuery(text) },
                )
            )
        }
    }

    private fun chipTexts(group: ChipGroup): List<String> =
        (0 until group.childCount).mapNotNull { index ->
            (group.getChildAt(index) as? Chip)?.text?.toString()
        }

    private fun createChip(
        text: String,
        showClose: Boolean,
        onClick: () -> Unit,
        onClose: (() -> Unit)? = null,
    ): Chip {
        val chip = layoutInflater.inflate(R.layout.item_search_chip, null, false) as Chip
        return chip.apply {
            this.text = text
            isCheckable = false
            isCloseIconVisible = showClose
            if (showClose) {
                setOnCloseIconClickListener { onClose?.invoke() }
            }
            setOnClickListener { onClick() }
        }
    }

    private fun applyChipQuery(text: String) {
        searchJob?.cancel()
        binding.searchSong.setQuery(text, notify = false)
        searchViewModel.commitQuery(text)
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
        binding.microSearch.setOnClickListener { voiceSearch.start() }
        binding.clearAllRecentSearches.setOnClickListener {
            searchViewModel.clearRecentQueries()
        }
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        tabMediator = null
        binding.searchResultsPager.adapter = null
        binding.rcvSearchPreview.adapter = null
        super.onDestroyView()
    }

    private enum class SearchChrome {
        Idle,
        Loading,
        Preview,
        Tabs,
    }
}
