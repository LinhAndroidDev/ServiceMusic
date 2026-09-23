package com.example.serviceandroid.custom

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.UserPlaylistAdapter
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment
import com.example.serviceandroid.data.playlist.UserPlaylist
import com.example.serviceandroid.databinding.LayoutBottomSheetPickPlaylistBinding
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetPickPlaylist :
    BaseBottomSheetDialogFragment<LayoutBottomSheetPickPlaylistBinding>() {

    private val viewModel by viewModels<BottomSheetPickPlaylistViewModel>()
    private val adapter = UserPlaylistAdapter()
    private var song: Song? = null

    override val layoutResId: Int
        get() = R.layout.layout_bottom_sheet_pick_playlist

    override fun initView() {
        song = arguments?.getParcelable(Constant.KEY_SONG)
        adapter.onClickItem = { playlist, _, _ -> addSongToPlaylist(playlist) }
        binding.rcvPlaylists.adapter = adapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { adapter.submit(it) }
            }
        }
    }

    override fun onClickView() {
        binding.createPlaylist.setOnClickListener { showCreatePlaylist() }
    }

    private fun showCreatePlaylist() {
        DialogCreatePlaylist().apply {
            onConfirm = onConfirm@{ title, isPublic ->
                val selected = song ?: return@onConfirm
                val host = activity as? MainActivity
                dismiss()
                host?.createPlaylistAndAddSong(title, isPublic, selected)
            }
        }.show(parentFragmentManager, "create_playlist")
    }

    private fun addSongToPlaylist(playlist: UserPlaylist) {
        val selected = song ?: return
        val host = activity as? MainActivity
        dismiss()
        host?.addSongToPlaylist(playlist, selected)
    }

    companion object {
        fun newInstance(song: Song): BottomSheetPickPlaylist {
            return BottomSheetPickPlaylist().apply {
                arguments = Bundle().apply {
                    putParcelable(Constant.KEY_SONG, song)
                }
            }
        }
    }
}
