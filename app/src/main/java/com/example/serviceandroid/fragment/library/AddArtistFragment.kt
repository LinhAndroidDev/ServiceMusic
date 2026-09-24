package com.example.serviceandroid.fragment.library

import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.AddArtistGridAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.data.artist.FollowMutationResult
import com.example.serviceandroid.databinding.FragmentAddArtistBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddArtistFragment : BaseFragment<FragmentAddArtistBinding>() {

    private val viewModel by viewModels<AddArtistViewModel>()
    private val adapter = AddArtistGridAdapter()

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentAddArtistBinding.inflate(inflater)

    override fun initView() {
        binding.searchBar.search.setHint(R.string.artist_add_search_hint)
        adapter.onClickSinger = { singer -> viewModel.toggleSelection(singer.id) }
        binding.rcvAddArtists.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rcvAddArtists.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submit(state.singers, state.selectedIds)
                    val empty = !state.isLoading && state.singers.isEmpty()
                    binding.rcvAddArtists.isVisible = !empty
                    binding.emptyAddArtists.isVisible = empty
                    binding.btnComplete.isEnabled = !state.isSaving
                }
            }
        }
    }

    override fun onClickView() {
        binding.closeAddArtist.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.searchBar.search.doAfterTextChanged { text ->
            binding.searchBar.removeText.isVisible = !text.isNullOrBlank()
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }
        binding.searchBar.removeText.setOnClickListener {
            binding.searchBar.search.setText("")
        }
        binding.btnComplete.setOnClickListener {
            viewModel.complete(::onComplete)
        }
    }

    private fun onComplete(result: AddArtistCompleteResult) {
        if (!isAdded) return
        when (result) {
            AddArtistCompleteResult.NoneSelected -> Toast.makeText(
                requireContext(),
                R.string.artist_add_select_required,
                Toast.LENGTH_SHORT,
            ).show()
            AddArtistCompleteResult.Success -> {
                Toast.makeText(
                    requireContext(),
                    R.string.artist_followed_toast,
                    Toast.LENGTH_SHORT,
                ).show()
                findNavController().popBackStack()
            }
            is AddArtistCompleteResult.Failed -> showFollowFailure(result.result)
        }
    }

    private fun showFollowFailure(result: FollowMutationResult) {
        when (result) {
            FollowMutationResult.Success -> Unit
            FollowMutationResult.RequiresLogin -> Toast.makeText(
                requireContext(),
                R.string.artist_login_message,
                Toast.LENGTH_LONG,
            ).show()
            FollowMutationResult.Offline -> Toast.makeText(
                requireContext(),
                R.string.artist_offline,
                Toast.LENGTH_LONG,
            ).show()
            is FollowMutationResult.Failure -> Toast.makeText(
                requireContext(),
                result.message.ifBlank { getString(R.string.artist_operation_failed) },
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
