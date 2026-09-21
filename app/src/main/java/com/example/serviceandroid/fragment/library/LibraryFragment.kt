package com.example.serviceandroid.fragment.library

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.serviceandroid.R
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.adapter.LibraryAdapter
import com.example.serviceandroid.adapter.ListenRecentAdapter
import com.example.serviceandroid.adapter.UserPlaylistAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.DialogCreatePlaylist
import com.example.serviceandroid.custom.VoiceSearch
import com.example.serviceandroid.databinding.FragmentLibraryBinding
import com.example.serviceandroid.fragment.downloaded.DownloadedSongsViewModel
import com.example.serviceandroid.fragment.favourite_song.FragmentFavouriteSongViewModel
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.model.Library
import com.example.serviceandroid.playback.PlaybackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    private val favouriteViewModel by activityViewModels<FragmentFavouriteSongViewModel>()
    private val downloadedViewModel by viewModels<DownloadedSongsViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private val recentHistoryViewModel by viewModels<RecentHistoryViewModel>()
    private val playlistViewModel by viewModels<PlaylistViewModel>()
    private lateinit var recentAdapter: ListenRecentAdapter
    private lateinit var playlistAdapter: UserPlaylistAdapter
    private val voiceSearch = VoiceSearch(this) { query ->
        findNavController().navigate(
            LibraryFragmentDirections.actionLibraryFragmentToFragmentSearchSong(query),
        )
    }

    override fun initView() {
        binding.header.title.text = "Thư viện"
        initLibrary()
        initRecentHistory()
        initPlaylists()
    }

    override fun onClickView() {
        binding.header.search.setOnClickListener {
            findNavController().navigate(R.id.action_libraryFragment_to_fragmentSearchSong)
        }
        binding.header.micro.setOnClickListener { voiceSearch.start() }
        binding.addPlaylist.setOnClickListener { showCreatePlaylist() }
    }

    private fun initRecentHistory() {
        recentAdapter = ListenRecentAdapter().apply {
            onClickItem = { song ->
                val songs = recentHistoryViewModel.uiState.value.previewSongs
                if (playbackViewModel.playFromVisibleList(requireContext(), songs, song.id)) {
                    MusicPlayerLauncher.open(
                        fragment = this@LibraryFragment,
                        songId = song.id,
                        preservePlayback = true,
                    )
                }
            }
            onClickSeeAll = {
                val navHostFragment =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
                navHostFragment.navController.navigate(
                    R.id.action_libraryFragment_to_recentHistoryFragment
                )
            }
        }
        binding.rcvListenRecent.adapter = recentAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                recentHistoryViewModel.uiState.collect { state ->
                    binding.recentHistoryProgress.isVisible = state.isLoading
                    binding.rcvListenRecent.isVisible = !state.isLoading && state.previewSongs.isNotEmpty()
                    binding.recentHistoryEmpty.isVisible = !state.isLoading && state.previewSongs.isEmpty()
                    recentAdapter.submit(state.previewSongs, state.showSeeAll)
                }
            }
        }
    }

    private fun initPlaylists() {
        playlistAdapter = UserPlaylistAdapter().apply {
            onClickItem = { playlist ->
                findNavController().navigate(
                    LibraryFragmentDirections.actionLibraryFragmentToPlaylistDetailFragment(
                        playlist.id,
                    ),
                )
            }
        }
        binding.rcvPlaylists.adapter = playlistAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playlistViewModel.playlists.collect { playlists ->
                    playlistAdapter.submit(playlists)
                    binding.rcvPlaylists.isVisible = playlists.isNotEmpty()
                    binding.playlistEmpty.isVisible = playlists.isEmpty()
                }
            }
        }
    }

    private fun showCreatePlaylist() {
        (activity as? MainActivity)?.ensureSignedInForPlaylist {
            DialogCreatePlaylist().apply {
                onConfirm = { title, isPublic ->
                    playlistViewModel.createPlaylist(title, isPublic) { result ->
                        (activity as? MainActivity)?.showPlaylistMutation(result)
                    }
                }
            }.show(parentFragmentManager, "create_playlist")
        }
    }

    private fun initLibrary() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    favouriteViewModel.favouriteCount,
                    downloadedViewModel.count,
                ) { favouriteCount, downloadedCount ->
                    favouriteCount to downloadedCount
                }.collect { (favouriteCount, downloadedCount) ->
                    val librarys = arrayListOf(
                        Library(R.drawable.favourite, "Bài hát yêu thích", favouriteCount, R.color.bg_blue),
                        Library(R.drawable.ic_download, "Đã tải", downloadedCount, R.color.bg_purple),
                        Library(R.drawable.ic_artist, "Nghệ sĩ", 0, R.color.bg_orange),
                        Library(R.drawable.ic_upload, "Upload", 0, R.color.yellow_dark),
                        Library(R.drawable.ic_mv, "MV", 0, R.color.bg_purple),
                    )
                    val libraryAdapter = LibraryAdapter(requireActivity())
                    libraryAdapter.items = librarys
                    libraryAdapter.onClickItem = { index ->
                        val navHostFragment =
                            requireActivity().supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
                        val navController = navHostFragment.navController
                        when (index) {
                            0 -> navController.navigate(R.id.favouriteSongFragment)
                            1 -> navController.navigate(R.id.downloadedSongsFragment)
                        }
                    }
                    binding.rcvLibrary.adapter = libraryAdapter
                }
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentLibraryBinding.inflate(inflater)
}
