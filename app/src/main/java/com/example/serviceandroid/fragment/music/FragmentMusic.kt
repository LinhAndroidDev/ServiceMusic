package com.example.serviceandroid.fragment.music

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.FragmentMusicBinding
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.model.Repeat
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.playback.PlaybackUiState
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.CustomAnimator
import com.example.serviceandroid.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@AndroidEntryPoint
class FragmentMusic : BaseFragment<FragmentMusicBinding>() {
    private val args: FragmentMusicArgs by navArgs()
    private val viewModel by viewModels<FragmentMusicViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private val fadeIn by lazy { AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_fade_in) }
    private val rotate45 by lazy { AnimationUtils.loadAnimation(requireActivity(), R.anim.rotation_45) }
    private var isFavourite: Boolean = false
    private var lastRenderedSongId: Int? = null

    override fun getFragmentBinding(inflater: LayoutInflater): FragmentMusicBinding {
        return FragmentMusicBinding.inflate(inflater)
    }

    override fun initView() {
        val idSong = if (args.idMusic == 0) {
            arguments?.getInt("id_music") ?: 0
        } else {
            args.idMusic
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavourite.collect {
                    isFavourite = it
                    if (it) {
                        binding.imgFavourite.setImageResource(R.drawable.ic_favourite_fill)
                        binding.imgFavourite.imageTintList =
                            ColorStateList.valueOf(requireContext().getColor(R.color.red))
                    } else {
                        binding.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
                        binding.imgFavourite.imageTintList =
                            ColorStateList.valueOf(requireContext().getColor(R.color.white))
                    }
                }
            }
        }

        CustomAnimator.rotationImage(binding.imgSong)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.imageCover.setRenderEffect(
                RenderEffect.createBlurEffect(
                    50.0f, 50.0f, Shader.TileMode.CLAMP
                )
            )
        }

        val repeat = viewModel.getTypeRepeat()
        binding.imgRepeat.setImageResource(repeat.value)

        initMusic(idSong)
    }

    private fun handleRepeat() {
        val repeat = viewModel.getTypeRepeat()
        val resourceImageId = when (repeat) {
            Repeat.NOT_REPEAT -> {
                viewModel.saveTypeRepeat(Repeat.REPEAT_ALL)
                R.drawable.ic_repeat_all
            }
            Repeat.REPEAT_ALL -> {
                viewModel.saveTypeRepeat(Repeat.REPEAT_ONE)
                R.drawable.ic_repeat_one
            }
            Repeat.REPEAT_ONE -> {
                viewModel.saveTypeRepeat(Repeat.NOT_REPEAT)
                R.drawable.ic_not_repeat
            }
        }
        binding.imgRepeat.setImageResource(resourceImageId)
        playbackViewModel.syncRepeatMode(requireContext())
    }

    override fun onClickView() {
        binding.backMusic.setOnClickListener {
            activity?.onBackPressed()
        }
        binding.imgNext.setOnClickListener {
            playbackViewModel.next(requireContext())
        }
        binding.imgPrevious.setOnClickListener {
            playbackViewModel.previous(requireContext())
        }

        binding.imgPlay.setOnClickListener {
            CustomAnimator.endAnimation(rotate45) {
                val st = playbackViewModel.playbackState.value
                if (!st.isPlaying) {
                    playbackViewModel.resume(requireContext())
                } else {
                    playbackViewModel.pause(requireContext())
                }
            }
            binding.imgPlay.startAnimation(rotate45)
        }

        binding.imgRepeat.setOnClickListener {
            handleRepeat()
        }

        binding.progressMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playbackViewModel.seekTo(requireContext(), progress)
                    setProgressTime(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.imgFavourite.setOnClickListener {
            val song = playbackViewModel.playbackState.value.currentSong ?: return@setOnClickListener
            if (!isFavourite) {
                viewModel.insertSong(song, DateUtils.getTimeCurrent()) {
                    if (!isAdded) return@insertSong
                    playbackViewModel.refreshMiniPlayerFavouriteForCurrentSong()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_added_favourite),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                DialogConfirm().apply {
                    title = song.title
                    onClickRemove = {
                        viewModel.deleteSongById(song.idSong) {
                            if (!this@FragmentMusic.isAdded) return@deleteSongById
                            playbackViewModel.refreshMiniPlayerFavouriteForCurrentSong()
                            Toast.makeText(
                                this@FragmentMusic.requireContext(),
                                this@FragmentMusic.getString(R.string.toast_removed_favourite),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.show(parentFragmentManager, "")
            }
        }
    }

    private fun initMusic(idSong: Int) {
        resetFavourite()
        val resolvedIndex = playbackViewModel.resolveQueueIndexForSongId(idSong)
        val song = playbackViewModel.getPlaylist()[resolvedIndex]
        val fromMini = playbackViewModel.consumePendingOpenFromMiniPlayer()
        val st = playbackViewModel.playbackState.value
        val keepPlaying = fromMini &&
            st.queueIndex == resolvedIndex &&
            st.hasActivePlayer

        if (!keepPlaying) {
            playbackViewModel.playSongAtIndex(requireContext(), resolvedIndex)
        }

        bindSongMetadata(song)
        viewModel.checkSongById(song.idSong)
    }

    private fun bindSongMetadata(song: Song) {
        lastRenderedSongId = song.idSong
        Glide.with(this)
            .load(song.avatar)
            .error(R.drawable.ic_circle)
            .placeholder(R.drawable.ic_circle)
            .into(binding.imgSong)
        binding.imageCover.setImageResource(song.avatar)
        binding.imgSong.startAnimation(fadeIn)
        binding.tvNameSong.text = song.title
        binding.tvNameSinger.text = song.nameSinger
        val st = playbackViewModel.playbackState.value
        val duration = st.durationMs
        if (duration > 0) {
            setMaxProgress(duration)
            setTotalTimeFromDuration(duration)
            val pos = st.positionMs.coerceIn(0, duration)
            binding.progressMusic.progress = pos
            setProgressTime(pos)
        }
        if (st.isPlaying) startMusic() else pauseMusic()
    }

    fun onPlaybackStateChanged(state: PlaybackUiState) {
        val song = state.currentSong ?: return
        if (song.idSong != lastRenderedSongId) {
            bindSongMetadata(song)
            viewModel.checkSongById(song.idSong)
        }
        if (state.durationMs > 0) {
            binding.progressMusic.max = state.durationMs
            binding.progressMusic.progress =
                state.positionMs.coerceIn(0, state.durationMs)
            setProgressTime(state.positionMs)
            setTotalTimeFromDuration(state.durationMs)
        }
        if (state.isPlaying) startMusic() else pauseMusic()
    }

    @SuppressLint("SimpleDateFormat")
    private fun setProgressTime(currentPosition: Int) {
        binding.tvProgressTime.text = SimpleDateFormat(Constants.MINUTES).format(currentPosition)
    }

    @SuppressLint("SimpleDateFormat")
    private fun setTotalTimeFromDuration(durationMs: Int) {
        binding.tvTotalTime.text = SimpleDateFormat(Constants.MINUTES).format(durationMs)
    }

    private fun setMaxProgress(duration: Int) {
        binding.progressMusic.apply {
            max = duration
        }
    }

    private fun startMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_pause_music)
    }

    private fun pauseMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_play_music)
    }

    private fun resetFavourite() {
        isFavourite = false
        binding.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
        binding.imgFavourite.imageTintList =
            ColorStateList.valueOf(requireContext().getColor(R.color.white))
    }
}
