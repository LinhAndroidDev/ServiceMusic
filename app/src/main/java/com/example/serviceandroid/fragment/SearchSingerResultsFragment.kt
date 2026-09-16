package com.example.serviceandroid.fragment

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.SearchSingerAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentSearchSongResultsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchSingerResultsFragment : BaseFragment<FragmentSearchSongResultsBinding>() {

    private val searchViewModel by viewModels<SearchViewModel>({ requireParentFragment() })
    private var singerAdapter: SearchSingerAdapter? = null

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSearchSongResultsBinding.inflate(inflater)

    override fun initView() {
        binding.tvSearchPageEmpty.setText(R.string.search_singers_empty)
        val adapter = SearchSingerAdapter().also { created ->
            created.onClickSinger = { singer ->
                searchViewModel.recordCurrentQuery()
                val action = FragmentSearchSongDirections
                    .actionFragmentSearchSongToSingerDetailFragment(singer.id)
                requireParentFragment().findNavController().navigate(action)
            }
            singerAdapter = created
        }
        binding.rcvSearchPage.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvSearchPage.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchViewModel.uiState.collect { state ->
                    adapter.submit(state.singers)
                    binding.rcvSearchPage.isVisible = state.singers.isNotEmpty()
                    binding.tvSearchPageEmpty.isVisible = state.singers.isEmpty()
                }
            }
        }
    }

    override fun onClickView() = Unit
}
