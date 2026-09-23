package com.example.serviceandroid.custom

import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment
import com.example.serviceandroid.databinding.LayoutBottomSheetPlaylistMenuBinding

class BottomSheetPlaylistMenu :
    BaseBottomSheetDialogFragment<LayoutBottomSheetPlaylistMenuBinding>() {

    var onAddSongs: (() -> Unit)? = null
    var onEditPlaylist: (() -> Unit)? = null
    var onDeletePlaylist: (() -> Unit)? = null

    override val layoutResId: Int
        get() = R.layout.layout_bottom_sheet_playlist_menu

    override fun initView() = Unit

    override fun onClickView() {
        binding.addSongs.setOnClickListener {
            val action = onAddSongs
            dismiss()
            action?.invoke()
        }
        binding.editPlaylist.setOnClickListener {
            val action = onEditPlaylist
            dismiss()
            action?.invoke()
        }
        binding.deletePlaylist.setOnClickListener {
            val action = onDeletePlaylist
            dismiss()
            action?.invoke()
        }
    }
}
