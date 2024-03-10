package com.example.serviceandroid.service

import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.databinding.ActivityMusicBinding
import com.example.serviceandroid.model.Song

class MusicActivity : BaseActivity() {
    private val binding by lazy { ActivityMusicBinding.inflate(layoutInflater) }
    private lateinit var timePlay: CountDownTimer
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var rotationAnimator: ObjectAnimator
    private var sds = 0
    private var minus = 0
    private var currentViewingAngle = 0f
    private var isRepeat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val song: Song = intent.getSerializableExtra(MainActivity.OBJECT_MUSIC) as Song
        Glide.with(this)
            .load(song.avatar)
            .error(R.mipmap.ic_launcher)
            .placeholder(R.mipmap.ic_launcher)
            .into(binding.imgSong)
        binding.tvNameSong.text = song.title
        binding.tvNameSinger.text = song.nameSinger
        binding.tvTotalTime.text = getTimeFromSeconds(song.time)

        binding.progressMusic.progress = 0

        playMusic(song)

        binding.backMusic.setOnClickListener {
            onBackPressed()
        }
    }

    private fun playMusic(song: Song) {
        rotationAnimator = ObjectAnimator.ofFloat(binding.imgSong, "rotation", 0f, 360f)
        rotationAnimator.duration = 15000 // Đặt thời gian hoàn thành xoay (10 giây ở đây)
        rotationAnimator.repeatCount = ObjectAnimator.INFINITE // Lặp vô hạn
        rotationAnimator.interpolator = LinearInterpolator()

        mediaPlayer = MediaPlayer.create(this, song.sing)

        binding.progressMusic.max = mediaPlayer!!.duration
        binding.progressMusic.progress = 0
        timePlay = object : CountDownTimer((mediaPlayer!!.duration).toLong(), 1000) {
            override fun onTick(p0: Long) {
                binding.progressMusic.progress = mediaPlayer!!.currentPosition
                if(sds == 59) {
                    minus += 1
                    sds = 0
                } else {
                    sds += 1
                }
                setProgressTime()
            }

            override fun onFinish() {
                minus = 0
                sds = 0
                binding.progressMusic.progress = 0
                if(isRepeat) {
                    mediaPlayer?.isLooping = true
                } else cancelMusic()
            }

        }

        startMusic()

        binding.imgPlay.setOnClickListener {
            isPlaying = !isPlaying
            if (isPlaying) startMusic() else cancelMusic()
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
                    Log.e("Tiến trình", "$progress")
                    minus = (progress/1000) / 60
                    sds = (progress/1000) % 60
                    setProgressTime()
                }
                if(progress >= binding.progressMusic.max) {
                    cancelMusic()
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })
    }

    private fun setProgressTime() {
        binding.tvProgressTime.text = if (sds >= 10) {
            "${minus}:${sds}"
        } else {
            "${minus}:0${sds}"
        }
    }

    private fun getTimeFromSeconds(time: Int): String {
        val minutes = time / 60
        val seconds = time % 60
        return if (seconds >= 10) {
            "${minutes}:${seconds}"
        } else {
            "${minutes}:0${seconds}"
        }
    }

    private fun startMusic() {
        if(binding.imgSong.rotation != 0f) {
            binding.imgSong.rotation = currentViewingAngle
        }
        binding.imgPlay.setImageResource(R.drawable.ic_pause_music)
        timePlay.start()
        mediaPlayer?.start()
        rotationAnimator.start()
    }

    private fun cancelMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_play_music)
        timePlay.cancel()
        mediaPlayer?.pause()
        rotationAnimator.cancel()
        currentViewingAngle = binding.imgSong.rotation
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