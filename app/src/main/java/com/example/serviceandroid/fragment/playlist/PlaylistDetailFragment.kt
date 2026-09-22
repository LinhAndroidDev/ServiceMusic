package com.example.serviceandroid.fragment.playlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.BottomSheetPlaylistMenu
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.data.playlist.PlaylistMutationResult
import com.example.serviceandroid.databinding.FragmentPlaylistDetailBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import com.example.serviceandroid.utils.loadSongThumbnail
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class PlaylistDetailFragment : BaseFragment<FragmentPlaylistDetailBinding>() {
    private val viewModel by viewModels<PlaylistDetailViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private lateinit var adapter: PagerNewReleaseAdapter

    override fun initView() {
        adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            onClickItem = { songId ->
                val songs = viewModel.songs.value
                if (playbackViewModel.playFromVisibleList(requireContext(), songs, songId)) {
                    MusicPlayerLauncher.open(
                        this@PlaylistDetailFragment,
                        songId,
                        preservePlayback = true,
                    )
                }
            }
            onClickMoreOption = { song ->
                val dialog = BottomSheetOptionMusic()
                dialog.removeFavourite = null
                dialog.onRemoveFromPlaylist = { removeSongFromPlaylist(song.id) }
                dialog.arguments = Bundle().apply {
                    putParcelable(Constant.KEY_SONG, song)
                    putBoolean(Constant.KEY_SHOW_REMOVE_FROM_PLAYLIST, true)
                }
                dialog.show(parentFragmentManager, "song_options")
            }
        }
        binding.rcvPlaylistSongs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.playlist, viewModel.songs) { playlist, songs ->
                    playlist to songs
                }.collect { (playlist, songs) ->
                    binding.playlistTitle.text =
                        playlist?.title ?: getString(R.string.playlist_section_title)
                    binding.numberSong.text = when {
                        playlist == null -> getString(R.string.playlist_song_count, songs.size)
                        playlist.isPublic -> getString(R.string.playlist_meta_public, songs.size)
                        else -> getString(R.string.playlist_meta_private, songs.size)
                    }
                    binding.notFoundSong.isVisible = songs.isEmpty()
                    binding.rcvPlaylistSongs.isVisible = songs.isNotEmpty()
                    bindCover(songs.firstOrNull()?.thumbnailUrl)
                    adapter.items = songs.toMutableList()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onClickView() {
        binding.backPlaylist.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.menuPlaylist.setOnClickListener { showPlaylistMenu() }
    }

    private fun bindCover(url: String?) {
        if (url.isNullOrBlank()) {
            binding.playlistCover.setImageResource(R.drawable.ic_playlist)
        } else {
            binding.playlistCover.loadSongThumbnail(url)
        }
    }

    private fun showPlaylistMenu() {
        val menu = BottomSheetPlaylistMenu()
        menu.onAddSongs = {
            findNavController().navigate(
                PlaylistDetailFragmentDirections
                    .actionPlaylistDetailFragmentToAddPlaylistSongsFragment(
                        viewModel.playlistId,
                    ),
            )
        }
        menu.onEditPlaylist = {
            findNavController().navigate(
                PlaylistDetailFragmentDirections
                    .actionPlaylistDetailFragmentToEditPlaylistFragment(
                        viewModel.playlistId,
                    ),
            )
        }
        menu.onDeletePlaylist = { confirmDeletePlaylist() }
        menu.show(parentFragmentManager, "playlist_menu")
    }

    private fun removeSongFromPlaylist(songId: String) {
        viewModel.removeSong(songId) { result ->
            if (result is PlaylistMutationResult.Success) {
                Toast.makeText(
                    requireContext(),
                    R.string.playlist_song_removed,
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                (activity as? MainActivity)?.showPlaylistMutation(result)
            }
        }
    }

    private fun confirmDeletePlaylist() {
        val dialog = DialogConfirm()
        dialog.title = getString(R.string.playlist_delete_title)
        dialog.message = getString(R.string.playlist_delete_message)
        dialog.confirmText = getString(R.string.playlist_delete_confirm)
        dialog.cancelText = getString(R.string.playlist_cancel)
        dialog.onClickRemove = {
            viewModel.deletePlaylist { result ->
                if (!isAdded) return@deletePlaylist
                if (result is PlaylistMutationResult.Success) {
                    Toast.makeText(
                        requireContext(),
                        R.string.playlist_deleted,
                        Toast.LENGTH_SHORT,
                    ).show()
                    findNavController().popBackStack()
                } else {
                    (activity as? MainActivity)?.showPlaylistMutation(result)
                }
            }
        }
        dialog.show(parentFragmentManager, "delete_playlist")
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentPlaylistDetailBinding.inflate(inflater)
}
