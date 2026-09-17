package com.example.serviceandroid.custom

import android.content.res.ColorStateList
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment
import com.example.serviceandroid.database.DownloadStatus
import com.example.serviceandroid.databinding.LayoutBottomSheetOptionMusicBinding
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.Constant
import com.example.serviceandroid.utils.Convert
import com.example.serviceandroid.utils.loadSongThumbnail
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
            binding.avatar.loadSongThumbnail(song.thumbnailUrl)
            binding.titleSong.text = song.title
            binding.tvNameSinger.text = song.nameSinger
            viewModel.checkSongById(song.id)
            viewModel.observeDownload(song.id)
        }

        lifecycleScope.launch {
            viewModel.isFavourite.collect { favourite ->
                isFavourite = favourite
                if (favourite) {
                    binding.imgFavourite.setImageResource(R.drawable.ic_favourite_fill)
                    binding.imgFavourite.imageTintList =
                        ColorStateList.valueOf(requireActivity().getColor(R.color.purple_1))
                    binding.tvFavourite.text = "Đã thêm vào thư viện"
                } else {
                    binding.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
                    binding.imgFavourite.imageTintList =
                        ColorStateList.valueOf(requireActivity().getColor(R.color.black))
                    binding.tvFavourite.text = "Thêm vào thư viện"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.downloadStatus.collect { status ->
                    bindDownloadStatus(status)
                }
            }
        }
    }

    private fun bindDownloadStatus(status: DownloadStatus?) {
        when (status) {
            DownloadStatus.COMPLETED -> {
                binding.tvDownload.setText(R.string.download_remove_action)
                binding.downloadSong.isEnabled = true
                binding.downloadSong.alpha = 1f
            }
            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                binding.tvDownload.setText(R.string.download_status_downloading)
                binding.downloadSong.isEnabled = false
                binding.downloadSong.alpha = 0.6f
            }
            DownloadStatus.FAILED, null -> {
                binding.tvDownload.setText(R.string.download_action)
                binding.downloadSong.isEnabled = true
                binding.downloadSong.alpha = 1f
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = (Convert.getWidthDevice(requireActivity()))
        }
    }

    override fun onClickView() {
        binding.addFavourite.setOnClickListener {
            if (!isFavourite) {
                songModel?.let {
                    (activity as? MainActivity)?.requestAddFavourite(it)
                }
            } else {
                val customRemove = removeFavourite
                if (customRemove != null) {
                    customRemove()
                } else {
                    val song = songModel ?: return@setOnClickListener
                    DialogConfirm().apply {
                        title = song.title
                        onClickRemove = {
                            (activity as? MainActivity)?.requestRemoveFavourite(song.id) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.toast_removed_favourite,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }.show(parentFragmentManager, "remove_favourite")
                }
                dismiss()
            }
        }

        binding.downloadSong.setOnClickListener {
            val song = songModel ?: return@setOnClickListener
            if (viewModel.downloadStatus.value == DownloadStatus.COMPLETED) {
                viewModel.removeDownload(song.id) {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.toast_removed_download),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@setOnClickListener
            }
            viewModel.enqueueDownload(
                song = song,
                onStarted = {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.download_started),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onAlreadyDownloaded = {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.download_already_done),
                        Toast.LENGTH_SHORT
                    ).show()
                },
            )
        }
    }
}
