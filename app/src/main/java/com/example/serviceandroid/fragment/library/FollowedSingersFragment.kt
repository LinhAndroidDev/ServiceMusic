package com.example.serviceandroid.fragment.library

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.serviceandroid.adapter.SearchSingerAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.data.artist.FollowedSingerRepository
import com.example.serviceandroid.databinding.FragmentFollowedSingersBinding
import com.example.serviceandroid.model.Singer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FollowedSingersFragment : BaseFragment<FragmentFollowedSingersBinding>() {

    @Inject
    lateinit var followedSingerRepository: FollowedSingerRepository

    private val adapter = SearchSingerAdapter()

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentFollowedSingersBinding.inflate(inflater)

    override fun initView() {
        adapter.onClickSinger = { singer ->
            findNavController().navigate(
                FollowedSingersFragmentDirections
                    .actionFollowedSingersFragmentToSingerDetailFragment(singer.id),
            )
        }
        binding.rcvFollowedSingers.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                followedSingerRepository.observeFollowed().collect { singers ->
                    binding.followedEmpty.isVisible = singers.isEmpty()
                    binding.rcvFollowedSingers.isVisible = singers.isNotEmpty()
                    adapter.submit(
                        singers.map { Singer(id = it.id, name = it.name, avatarUrl = it.avatarUrl, description = "") },
                    )
                }
            }
        }
    }

    override fun onClickView() {
        binding.backFollowedSingers.setOnClickListener {
            activity?.onBackPressed()
        }
    }
}
