package com.example.serviceandroid

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.ActivityMusicBinding
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.service.MusicService
import com.example.serviceandroid.utils.CustomAnimator
import com.example.serviceandroid.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@AndroidEntryPoint
@Suppress("DEPRECATION")
class MusicActivity : BaseActivity<ActivityMusicBinding>(), PlayCallback {
    private val viewModel by viewModels<MusicViewModel>()
    private val timePlay = Handler(Looper.getMainLooper())
    override var mediaPlayer: MediaPlayer? = null
    override var isPlaying = true
    override var isRepeat = false
    override var isFinish = false
    override var dragToEnd = false
    override var isFavourite = false
    override var indexSong = 0
    private val fadeIn by lazy { AnimationUtils.loadAnimation(this, R.anim.anim_fade_in) }
    private val rotate45 by lazy { AnimationUtils.loadAnimation(this, R.anim.rotation_45) }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            mSong = intent.getParcelableExtra<Song>(Constants.OBJECT_SONG) as Song
            isPlaying = intent.getBooleanExtra(Constants.STATUS_PLAYING, false)
            handlerActionMusic(intent.getSerializableExtra(Constants.ACTION_MUSIC) as Action)
//            handleLayoutMusic(intent.getSerializableExtra(Constants.ACTION_MUSIC) as Action)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(Constants.SEND_DATA_TO_ACTIVITY))

        val idSong = intent.getIntExtra(MainActivity.ID_MUSIC, 0)
        lifecycleScope.launch {
            viewModel.isFavourite.collect {
                isFavourite = it
                binding.imgFavourite.setImageResource(if (it) R.drawable.ic_favourite_fill else R.drawable.ic_favourite_thin)
            }
        }
        Data.listMusic().filter { it.idSong == idSong }.forEach {
            indexSong = Data.listMusic().indexOf(it)
        }
        CustomAnimator.rotationImage(binding.imgSong)
        onClickView()
        initMusic()
    }

    @SuppressLint("SetTextI18n")
    private fun startServiceMusic(song: Song) {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(MainActivity.MESSAGE_MAIN, song)
        startService(intent)
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
                    if(progress == binding.progressMusic.max) {
                        dragToEnd = true
                    }
                }
                if (isFinish) {
                    handlerActionMusic(Action.ACTION_FINISH)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })

        binding.imgFavourite.setOnClickListener {
            if (!isFavourite) {
                isFavourite = true
                binding.imgFavourite.setImageResource(R.drawable.ic_favourite_fill)
                val mSong = Data.listMusic()[indexSong]
                viewModel.insertSong(mSong, DateUtils.getTimeCurrent()) {
                    Toast.makeText(
                        this@MusicActivity,
                        "Đã thêm vào bài hát yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                DialogConfirm().apply {
                    title = Data.listMusic()[indexSong].title
                    onClickRemove = {
                        viewModel.deleteSongById(Data.listMusic()[indexSong].idSong) {
                            Toast.makeText(
                                this@MusicActivity,
                                "Đã xoá khỏi bài hát yêu thích",
                                Toast.LENGTH_SHORT
                            ).show()
                            isFavourite = false
                        }
                    }
                }.show(supportFragmentManager, "")
            }
        }
    }

    private fun initMusic() {
        resetMusic()
        val song = Data.listMusic()[indexSong]
        Glide.with(this)
            .load(song.avatar)
            .error(R.drawable.ic_circle)
            .placeholder(R.drawable.ic_circle)
            .into(binding.imgSong)
        binding.imgSong.startAnimation(fadeIn)
        binding.tvNameSong.text = song.title
        binding.tvNameSinger.text = song.nameSinger
        lifecycleScope.launch { viewModel.checkSongById(Data.listMusic()[indexSong].idSong) }

        playMusic(song)
    }

    @SuppressLint("SimpleDateFormat")
    private fun playMusic(song: Song) {
        startServiceMusic(song)
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
                if(dragToEnd) {
                    dragToEnd = false
                    handlerActionMusic(Action.ACTION_FINISH)
                }
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

    /**
     * Send Action To Notification Music
     */
    private fun sendActionToService(action: Action) {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.RECEIVER_ACTION_MUSIC, action)
        startService(intent)
    }

    @SuppressLint("SimpleDateFormat")
    private fun setProgressTime() {
        binding.tvProgressTime.text = SimpleDateFormat(Constants.MINUTES).format(mediaPlayer?.currentPosition)
    }

    private fun resetMusic() {
        mediaPlayer?.reset()
    }

    private fun startMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_pause_music)
        mediaPlayer?.start()
        isPlaying = true
        sendActionToService(Action.ACTION_RESUME)
    }

    private fun pauseMusic() {
        binding.imgPlay.setImageResource(R.drawable.ic_play_music)
        mediaPlayer?.pause()
        isPlaying = false
        sendActionToService(Action.ACTION_PAUSE)
    }

    private fun previousSong() {
        isPlaying = true
        resetFavourite()
        if (indexSong > 0) {
            indexSong--
            initMusic()
        } else {
            indexSong = Data.listMusic().size - 1
        }
        initMusic()
    }

    private fun nextSong() {
        isPlaying = true
        resetFavourite()
        if (indexSong < Data.listMusic().size - 1) {
            indexSong++
        } else {
            indexSong = 0
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

    private fun resetFavourite() {
        isFavourite = false
        binding.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerActionMusic(Action.ACTION_PAUSE)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivityMusicBinding.inflate(inflater)

}