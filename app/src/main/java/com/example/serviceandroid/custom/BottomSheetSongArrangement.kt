package com.example.serviceandroid.custom

import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment
import com.example.serviceandroid.database.repository.ArrangeMusic
import com.example.serviceandroid.databinding.LayoutBottomSheetSongArrangementBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetSongArrangement:
    BaseBottomSheetDialogFragment<LayoutBottomSheetSongArrangementBinding>() {

    private val viewModel by viewModels<BottomSheetSongArrangementViewModel>()
    var onClickChangeState: (() -> Unit)? = null

    override fun initView() {
        viewModel.shared.getTypeArrangement().let { type ->
            when(type) {
                ArrangeMusic.NEWEST -> {
                    showCheckArrange(binding.checkNewest)
                }

                ArrangeMusic.OLDEST -> {
                    showCheckArrange(binding.checkOldest)
                }

                ArrangeMusic.BY_NAME_SONG -> {
                    showCheckArrange(binding.checkByNamSong)
                }

                ArrangeMusic.BY_NAME_SINGLE -> {
                    showCheckArrange(binding.checkByNameSingle)
                }
            }
        }
    }

    private fun showCheckArrange(check: ImageView) {
        binding.checkNewest.isVisible = false
        binding.checkOldest.isVisible = false
        binding.checkByNamSong.isVisible = false
        binding.checkByNameSingle.isVisible = false
        check.isVisible = true
    }

    override fun onClickView() {
        binding.newest.setOnClickListener {
            viewModel.saveStateArrange(ArrangeMusic.NEWEST)
            handleActionClick()
        }

        binding.oldest.setOnClickListener {
            viewModel.saveStateArrange(ArrangeMusic.OLDEST)
            handleActionClick()
        }

        binding.byNameSong.setOnClickListener {
            viewModel.saveStateArrange(ArrangeMusic.BY_NAME_SONG)
            handleActionClick()
        }

        binding.byNameSingle.setOnClickListener {
            viewModel.saveStateArrange(ArrangeMusic.BY_NAME_SINGLE)
            handleActionClick()
        }
    }

    private fun handleActionClick() {
        onClickChangeState?.invoke()
        dismiss()
    }

    override val layoutResId: Int
        get() = R.layout.layout_bottom_sheet_song_arrangement

    override fun onBottomSheetExpanded() {
        super.onBottomSheetExpanded()
        // Logic khi BottomSheet mở rộng
    }

    override fun onBottomSheetCollapsed() {
        super.onBottomSheetCollapsed()
        // Logic khi BottomSheet thu gọn
    }
}