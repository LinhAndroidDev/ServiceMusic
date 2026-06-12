package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
import com.example.serviceandroid.databinding.FragmentSearchSongBinding
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentSearchSong : BaseFragment<FragmentSearchSongBinding>() {

    private val searchViewModel by viewModels<SearchViewModel>()
    private var searchJob: Job? = null
    private var searchAdapter: PagerNewReleaseAdapter? = null

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongBinding.inflate(inflater)

    override fun initView() {
        binding.searchSong.showActionSearch()

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
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchViewModel.results.collect { results ->
                    val showResults = results.isNotEmpty()
                    binding.rcvSearchResults.isVisible = showResults
                    binding.contentView.isVisible = !showResults
                    if (showResults) {
                        ensureSearchAdapter().items = ArrayList(results)
                        binding.rcvSearchResults.adapter = searchAdapter
                        searchAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun ensureSearchAdapter(): PagerNewReleaseAdapter {
        searchAdapter?.let { return it }
        val adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL)
        adapter.onClickItem = { songId ->
            val action = FragmentSearchSongDirections.actionFragmentSearchSongToFragmentMusic(songId = songId)
            findNavController().navigate(action)
        }
        adapter.onClickMoreOption = { song -> showMoreOptions(song) }
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
