package com.example.serviceandroid.fragment.playlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentAddPlaylistSongsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class AddPlaylistSongsFragment : BaseFragment<FragmentAddPlaylistSongsBinding>() {
    private val viewModel by viewModels<AddPlaylistSongsViewModel>()
    private lateinit var adapter: PagerNewReleaseAdapter

    override fun initView() {
        binding.searchBar.search.setHint(R.string.playlist_search_hint)
        adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            showMoreOption = false
            onClickItem = onClickItem@{ songId ->
                val song = viewModel.uiState.value.songs.find { it.id == songId } ?: return@onClickItem
                viewModel.addSong(song) { result ->
                    (activity as? MainActivity)?.showPlaylistMutation(
                        result,
                        viewModel.currentPlaylistTitle(),
                    )
                }
            }
        }
        binding.rcvAddSongs.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.items = state.songs.toMutableList()
                    adapter.notifyDataSetChanged()
                    val empty = !state.isLoading && state.songs.isEmpty()
                    binding.rcvAddSongs.isVisible = !empty
                    binding.emptyAddSongs.isVisible = empty
                    binding.rcvAddSongs.post { bindAddedState(state.addedSongIds) }
                }
            }
        }
    }

    override fun onClickView() {
        binding.backAddSongs.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.searchBar.search.doAfterTextChanged { text ->
            binding.searchBar.removeText.isVisible = !text.isNullOrBlank()
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }
        binding.searchBar.removeText.setOnClickListener {
            binding.searchBar.search.setText("")
        }
    }

    private fun bindAddedState(addedIds: Set<String>) {
        adapter.items.forEachIndexed { index, song ->
            val holder = binding.rcvAddSongs.findViewHolderForAdapterPosition(index)
                ?: return@forEachIndexed
            holder.itemView.alpha = if (song.id in addedIds) 0.45f else 1f
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentAddPlaylistSongsBinding.inflate(inflater)
}
