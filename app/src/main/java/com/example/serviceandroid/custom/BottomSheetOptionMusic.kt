package com.example.serviceandroid.custom

import android.view.View
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment
import com.example.serviceandroid.databinding.LayoutBottomSheetOptionMusicBinding
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.Constant
import com.example.serviceandroid.utils.Convert
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class BottomSheetOptionMusic :
    BaseBottomSheetDialogFragment<LayoutBottomSheetOptionMusicBinding>() {
    override val layoutResId: Int
        get() = R.layout.layout_bottom_sheet_option_music

    override fun initView() {
        val song: Song? = arguments?.getParcelable(Constant.KEY_SONG)
        song?.let {
            Glide.with(requireActivity())
                .load(song.avatar)
                .into(binding.avatar)
            binding.titleSong.text = song.title
            binding.tvNameSinger.text = song.nameSinger
        }
    }

    override fun onStart() {
        super.onStart()

        // Lấy dialog và view chính của BottomSheet
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        bottomSheet?.let {
            // Thiết lập BottomSheetBehavior
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = (Convert.getWidthDevice(requireActivity()))
        }
    }

    override fun onClickView() {

    }
}