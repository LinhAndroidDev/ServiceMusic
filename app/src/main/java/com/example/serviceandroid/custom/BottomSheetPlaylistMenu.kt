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
            dismiss()
            onAddSongs?.invoke()
        }
        binding.editPlaylist.setOnClickListener {
            dismiss()
            onEditPlaylist?.invoke()
        }
        binding.deletePlaylist.setOnClickListener {
            dismiss()
            onDeletePlaylist?.invoke()
        }
    }
}
