package com.example.serviceandroid.fragment

import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.SearchResultsPagerAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.data.search.SearchQuery
import com.example.serviceandroid.databinding.FragmentSearchSongBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentSearchSong : BaseFragment<FragmentSearchSongBinding>() {

    private val searchViewModel by viewModels<SearchViewModel>()
    private var searchJob: Job? = null
    private var tabMediator: TabLayoutMediator? = null

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongBinding.inflate(inflater)

    override fun initView() {
        setupResultsPager()

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
                    showLoading = false,
                )
            } else if (!searchViewModel.uiState.value.hasResults ||
                searchViewModel.uiState.value.query != query.trim()
            ) {
                bindSearchChrome(
                    showSuggestions = false,
                    showResults = false,
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

    private fun applyState(state: SearchUiState) {
        bindIdleChips(state)
        val querying = state.query.isNotBlank() || binding.searchSong.queryText().isNotBlank()
        when {
            !querying -> bindSearchChrome(
                showSuggestions = true,
                showResults = false,
                showLoading = false,
            )
            state.isSearching -> bindSearchChrome(
                showSuggestions = false,
                showResults = state.hasResults,
                showLoading = !state.hasResults,
            )
            else -> bindSearchChrome(
                showSuggestions = false,
                showResults = true,
                showLoading = false,
            )
        }
    }

    private fun bindSearchChrome(
        showSuggestions: Boolean,
        showResults: Boolean,
        showLoading: Boolean,
    ) {
        binding.contentView.isVisible = showSuggestions
        binding.searchResultsContainer.isVisible = showResults
        binding.searchProgress.isVisible = showLoading
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
        searchViewModel.search(text)
    }

    override fun onClickView() {
        binding.backSearch.setOnClickListener { activity?.onBackPressed() }
        binding.clearAllRecentSearches.setOnClickListener {
            searchViewModel.clearRecentQueries()
        }
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        tabMediator = null
        binding.searchResultsPager.adapter = null
        super.onDestroyView()
    }
}
