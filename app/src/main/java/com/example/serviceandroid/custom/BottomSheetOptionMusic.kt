package com.example.serviceandroid.custom

import android.content.res.ColorStateList
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment
import com.example.serviceandroid.databinding.LayoutBottomSheetOptionMusicBinding
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.Constant
import com.example.serviceandroid.utils.Convert
import com.example.serviceandroid.utils.DateUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetOptionMusic :
    BaseBottomSheetDialogFragment<LayoutBottomSheetOptionMusicBinding>() {
    private val viewModel by viewModels<BottomSheetOptionMusicViewModel>()
    private var isFavourite = false
    private var songModel: Song? = null
    var removeFavourite: (() -> Unit)? = null

    override val layoutResId: Int
        get() = R.layout.layout_bottom_sheet_option_music

    override fun initView() {
        val song: Song? = arguments?.getParcelable(Constant.KEY_SONG)
        song?.let {
            songModel = it
            Glide.with(requireActivity())
                .load(song.avatar)
                .into(binding.avatar)
            binding.titleSong.text = song.title
            binding.tvNameSinger.text = song.nameSinger
            viewModel.checkSongById(song.idSong)
        }

        lifecycleScope.launch {
            viewModel.isFavourite.collect { favourite ->
                isFavourite = favourite
                if(favourite) {
                    binding.imgFavourite.setImageResource(R.drawable.ic_favourite_fill)
                    binding.imgFavourite.imageTintList = ColorStateList.valueOf(requireActivity().getColor(R.color.purple_1))
                    binding.tvFavourite.text = "Đã thêm vào thư viện"
                } else {
                    binding.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
                    binding.imgFavourite.imageTintList = ColorStateList.valueOf(requireActivity().getColor(R.color.black))
                    binding.tvFavourite.text = "Thêm vào thư viện"
                }
            }
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
        binding.addFavourite.setOnClickListener {
            if(!isFavourite) {
                songModel?.let {
                    viewModel.insertSong(it, DateUtils.getTimeCurrent()) {
                        viewModel.checkSongById(songModel?.idSong ?: 0)
                        Toast.makeText(requireActivity(), "Đã thêm vào bài hát yêu thích", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                removeFavourite?.invoke()
                dismiss()
            }
        }
    }
}