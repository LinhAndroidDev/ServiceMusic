package com.example.serviceandroid

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.databinding.ActivityMusicBinding
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.CustomAnimator
import java.text.SimpleDateFormat

@Suppress("DEPRECATION")
class MusicActivity : BaseActivity<ActivityMusicBinding>() {
    private val timePlay = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = true
    private var isRepeat = false
    private var isFinish = false
    private var index = 0
    private val fadeIn by lazy { AnimationUtils.loadAnimation(this, R.anim.anim_fade_in) }
    private val rotate45 by lazy { AnimationUtils.loadAnimation(this, R.anim.rotation_45) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        changeColorStatusBar(Color.BLACK)
        index = intent.getIntExtra(MainActivity.INDEX_MUSIC, 0)
        CustomAnimator.rotationImage(binding.imgSong)
        onClickView()
        initMusic()
    }

    /**
     * Catch Click View Components Event
     */
    private fun onClickView() {
        binding.backMusic.setOnClickListener {
            onBackPressed()
        }
        binding.imgNext.setOnClickListener {
            handlerActionMusic(Action.ACTION_NEXT)
        }
        binding.imgPrevious.setOnClickListener {
            handlerActionMusic(Action.ACTION_PREVIOUS)
        }

        binding.imgPlay.setOnClickListener {
            isPlaying = if (!isPlaying) {
                handlerActionMusic(Action.ACTION_START)
                true
            } else {
                handlerActionMusic(Action.ACTION_PAUSE)
                false
            }
            binding.imgPlay.startAnimation(rotate45)
        }

        binding.imgRepeat.setOnClickListener {
            isRepeat = !isRepeat
            binding.imgRepeat.setImageResource(
                if (isRepeat) R.drawable.ic_repeat_one else R.drawable.ic_repeat
            )
        }

        binding.progressMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    setProgressTime()
                }
                if (isFinish) {
                    handlerActionMusic(Action.ACTION_FINISH)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })
    }

    private fun initMusic() {
        resetMusic()
        val song = Data.listMusic()[index]
        Glide.with(this)
            .load(song.avatar)
            .error(R.drawable.ic_circle)
            .placeholder(R.drawable.ic_circle)
            .into(binding.imgSong)
        binding.imgSong.startAnimation(fadeIn)
        binding.tvNameSong.text = song.title
        binding.tvNameSinger.text = song.nameSinger

        playMusic(song)
    }

    @SuppressLint("SimpleDateFormat")
    private fun playMusic(song: Song) {
        mediaPlayer = MediaPlayer.create(this, song.sing)
        binding.progressMusic.apply {
            max = mediaPlayer!!.duration
            progress = 0
        }
        binding.tvTotalTime.text = SimpleDateFormat(Constants.MINUTES)
            .format(mediaPlayer!!.duration)
        resetTimer()
        handlerActionMusic(Action.ACTION_START)
    }

    private fun resetTimer() {
        timePlay.postDelayed(object : Runnable {
            override fun run() {
                if (isPlaying) {
                    binding.progressMusic.progress = mediaPlayer!!.currentPosition
                    setProgressTime()
                    mediaPlayer?.setOnCompletionListener {
                        isFinish = true
                    }
                }
                timePlay.postDelayed(this, 1000)
            }
        }, 0)
    }

    /**
     * Handle Music Playing Actions Here
     */
    private fun handlerActionMusic(action: Action) {
        when (action) {
            Action.ACTION_START -> startMusic()
            Action.ACTION_PAUSE -> pauseMusic()
            Action.ACTION_RESUME -> startMusic()
            Action.ACTION_NEXT -> nextSong()
            Action.ACTION_PREVIOUS -> previousSong()
            Action.ACTION_FINISH -> finishMusic()
            else -> {}
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setProgressTime() {
        binding.tvProgressTime.text = SimpleDateFormat(Constants.MINUTES).format(mediaPlayer?.currentPosition)
    }

    private fun resetMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_pause_music)
        mediaPlayer?.start()
        isPlaying = true
    }

    private fun pauseMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_play_music)
        mediaPlayer?.pause()
        isPlaying = false
    }

    private fun previousSong() {
        isPlaying = true
        if (index > 0) {
            index--
            initMusic()
        } else {
            index = Data.listMusic().size - 1
        }
        initMusic()
    }

    private fun nextSong() {
        isPlaying = true
        if (index < Data.listMusic().size - 1) {
            index++
        } else {
            index = 0
        }
        initMusic()
    }

    private fun finishMusic() {
        isFinish = false
        if (isRepeat) {
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            resetTimer()
        } else {
            mediaPlayer?.isLooping = false
            handlerActionMusic(Action.ACTION_NEXT)
        }
    }

    override fun onStop() {
        super.onStop()
        handlerActionMusic(Action.ACTION_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerActionMusic(Action.ACTION_PAUSE)
    }

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivityMusicBinding.inflate(inflater)

}