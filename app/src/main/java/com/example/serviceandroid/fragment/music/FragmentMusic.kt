package com.example.serviceandroid.fragment.music

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaPlayer
import android.os.Build
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.MusicViewModel
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.FragmentMusicBinding
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.utils.CustomAnimator
import com.example.serviceandroid.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@AndroidEntryPoint
class FragmentMusic : BaseFragment<FragmentMusicBinding>() {
    private val viewModel by viewModels<MusicViewModel>()
    private val fadeIn by lazy { AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_fade_in) }
    private val rotate45 by lazy { AnimationUtils.loadAnimation(requireActivity(), R.anim.rotation_45) }
    private var isFavourite: Boolean = false

    override fun getFragmentBinding(inflater: LayoutInflater): FragmentMusicBinding {
        return FragmentMusicBinding.inflate(inflater)
    }

    override fun initView() {
        val args: FragmentMusicArgs by navArgs()
        val idSong = args.idMusic
        lifecycleScope.launch {
            viewModel.isFavourite.collect {
                isFavourite = it
                binding.imgFavourite.setImageResource(if (it) R.drawable.ic_favourite_fill else R.drawable.ic_favourite_thin)
            }
        }
        Data.listMusic().filter { it.idSong == idSong }.forEach {
            (activity as MainActivity).indexSong = Data.listMusic().indexOf(it)
        }
        CustomAnimator.rotationImage(binding.imgSong)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.imageCover.setRenderEffect(
                RenderEffect.createBlurEffect(
                    50.0f, 50.0f, Shader.TileMode.CLAMP
                )
            )
        }

        initMusic()
    }

    /**
     * Catch Click View Components Event
     */
    override fun onClickView() {
        binding.backMusic.setOnClickListener {
            activity?.onBackPressed()
        }
        binding.imgNext.setOnClickListener {
            (activity as MainActivity).handlerActionMusic(Action.ACTION_NEXT)
        }
        binding.imgPrevious.setOnClickListener {
            (activity as MainActivity).handlerActionMusic(Action.ACTION_PREVIOUS)
        }

        binding.imgPlay.setOnClickListener {
            val act = activity as MainActivity
            act.isPlaying = if (!act.isPlaying) {
                act.handlerActionMusic(Action.ACTION_START)
                true
            } else {
                act.handlerActionMusic(Action.ACTION_PAUSE)
                false
            }
            binding.imgPlay.startAnimation(rotate45)
        }

        binding.imgRepeat.setOnClickListener {
            val act = activity as MainActivity
            act.isRepeat = !act.isRepeat
            binding.imgRepeat.setImageResource(
                if (act.isRepeat) R.drawable.ic_repeat_one else R.drawable.ic_repeat
            )
        }

        binding.progressMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                val act = activity as MainActivity
                if (fromUser) {
                    act.mediaPlayer?.seekTo(progress)
                    setProgressTime(act.mediaPlayer!!.currentPosition)
                    if(progress == binding.progressMusic.max) {
                        act.dragToEnd = true
                    }
                }
                if (act.isFinish) {
                    act.handlerActionMusic(Action.ACTION_FINISH)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })

        binding.imgFavourite.setOnClickListener {
            if (!isFavourite) {
                isFavourite = true
                val mSong = Data.listMusic()[(activity as MainActivity).indexSong]
                viewModel.insertSong(mSong, DateUtils.getTimeCurrent()) {
                    viewModel.checkSongById(mSong.idSong)
                    Toast.makeText(
                        requireActivity(),
                        "Đã thêm vào bài hát yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                DialogConfirm().apply {
                    title = Data.listMusic()[(activity as MainActivity).indexSong].title
                    onClickRemove = {
                        viewModel.deleteSongById(Data.listMusic()[(activity as MainActivity).indexSong].idSong) {
                            Toast.makeText(
                                requireActivity(),
                                "Đã xoá khỏi bài hát yêu thích",
                                Toast.LENGTH_SHORT
                            ).show()
                            isFavourite = false
                        }
                    }
                }.show(parentFragmentManager, "")
            }
        }
    }

    fun initMusic() {
        resetFavourite()
        (activity as MainActivity).resetMusic()
        val song = Data.listMusic()[(activity as MainActivity).indexSong]
        Glide.with(this)
            .load(song.avatar)
            .error(R.drawable.ic_circle)
            .placeholder(R.drawable.ic_circle)
            .into(binding.imgSong)
        binding.imageCover.setImageResource(song.avatar)
        binding.imgSong.startAnimation(fadeIn)
        binding.tvNameSong.text = song.title
        binding.tvNameSinger.text = song.nameSinger
        viewModel.checkSongById(Data.listMusic()[(activity as MainActivity).indexSong].idSong)

        (activity as MainActivity).playMusic(song)
    }

    internal fun updateProgressMusic(currentPosition: Int) {
        binding.progressMusic.progress = currentPosition
        setProgressTime(currentPosition)
    }

    @SuppressLint("SimpleDateFormat")
    private fun setProgressTime(currentPosition: Int) {
        binding.tvProgressTime.text = SimpleDateFormat(Constants.MINUTES).format(currentPosition)
    }

    @SuppressLint("SimpleDateFormat")
    internal fun setTotalTime(mediaPlayer: MediaPlayer) {
        binding.tvTotalTime.text = SimpleDateFormat(Constants.MINUTES).format(mediaPlayer.duration)
    }

    internal fun setMaxProgress(duration: Int) {
        binding.progressMusic.apply {
            max = duration
            progress = 0
        }
    }

    internal fun startMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_pause_music)
    }

    internal fun pauseMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_play_music)
    }

    private fun resetFavourite() {
        isFavourite = false
        binding.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
    }
}