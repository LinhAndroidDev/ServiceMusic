package com.example.serviceandroid

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.databinding.ActivityMusicBinding
import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Song
import java.text.SimpleDateFormat

@Suppress("DEPRECATION")
class MusicActivity : BaseActivity() {
    private val binding by lazy { ActivityMusicBinding.inflate(layoutInflater) }
    private var timePlay: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = true
    private lateinit var rotationAnimator: ObjectAnimator
    private var currentViewingAngle = 0f
    private var isRepeat = false
    private var isFinish = false
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        index = intent.getIntExtra(MainActivity.INDEX_MUSIC, 0)

        rotationImage()

        binding.backMusic.setOnClickListener {
            onBackPressed()
        }
        binding.imgNext.setOnClickListener {
            if(index < Data.listMusic().size - 1) {
                index++
            } else {
                index = 0
            }
            resetMusic()
        }
        binding.imgPrevious.setOnClickListener {
            if(index > 0) {
                index--
                resetMusic()
            } else {
                index = Data.listMusic().size - 1
            }
            resetMusic()
        }

        resetMusic()
    }

    private fun rotationImage() {
        rotationAnimator = ObjectAnimator.ofFloat(binding.imgSong, "rotation", 0f, 360f)
        rotationAnimator.duration = 20000 // Đặt thời gian hoàn thành xoay (10 giây ở đây)
        rotationAnimator.repeatCount = ValueAnimator.INFINITE // Lặp vô hạn
        rotationAnimator.repeatMode = ValueAnimator.RESTART
        rotationAnimator.interpolator = LinearInterpolator()
        rotationAnimator.start()
    }

    private fun resetMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        val song = Data.listMusic()[index]
        Glide.with(this)
            .load(song.avatar)
            .error(R.mipmap.ic_launcher)
            .placeholder(R.mipmap.ic_launcher)
            .into(binding.imgSong)
        binding.tvNameSong.text = song.title
        binding.tvNameSinger.text = song.nameSinger

        playMusic(song)
    }

    @SuppressLint("SimpleDateFormat")
    private fun playMusic(song: Song) {
        mediaPlayer = MediaPlayer.create(this, song.sing)
        binding.progressMusic.max = mediaPlayer!!.duration
        binding.progressMusic.progress = 0
        binding.tvTotalTime.text = SimpleDateFormat("mm:ss").format(mediaPlayer!!.duration)
        resetTimer()
        startMusic()

        binding.imgPlay.setOnClickListener {
            isPlaying = if (!isPlaying) {
                startMusic()
                true
            } else {
                cancelMusic()
                false
            }
        }

        binding.imgRepeat.setOnClickListener {
            isRepeat = !isRepeat
            binding.imgRepeat.setImageResource(
                if(isRepeat) R.drawable.ic_repeat_one else R.drawable.ic_repeat
            )
        }

        binding.progressMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    mediaPlayer?.seekTo(progress)
                    setProgressTime()
                }
                if(isFinish) {
                    if(isRepeat) {
                        timePlay = null
                        resetMusic()
                        timePlay?.start()
                    } else {
                        cancelMusic()
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })
    }

    private fun resetTimer() {
        timePlay = object : CountDownTimer((mediaPlayer!!.duration).toLong(), 1000) {
            override fun onTick(p0: Long) {
                binding.progressMusic.progress = mediaPlayer!!.currentPosition
                setProgressTime()
                mediaPlayer?.setOnCompletionListener {
                    isFinish = true
                }
            }

            override fun onFinish() {

            }

        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setProgressTime() {
        binding.tvProgressTime.text = SimpleDateFormat("mm:ss").format(mediaPlayer?.currentPosition)
    }

    private fun startMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_pause_music)
        timePlay?.start()
        mediaPlayer?.start()
    }

    private fun cancelMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_play_music)
        timePlay?.cancel()
        mediaPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        cancelMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelMusic()
    }
}