package com.example.serviceandroid

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.custom.ActionBottomBar
import com.example.serviceandroid.databinding.ActivityMainBinding
import com.example.serviceandroid.fragment.music.FragmentMusic
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.service.MusicService
import com.example.serviceandroid.utils.getCurrentFragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
@Suppress("DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding>(), PlayCallback {
    private var doubleBackToExitPressedOnce = false
    private lateinit var mSong: Song
    private val timePlay = Handler(Looper.getMainLooper())
    override var mediaPlayer: MediaPlayer? = null
    override var isPlaying = false
    override var isRepeat = false
    override var isFinish = false
    override var dragToEnd = false
    override var indexSong = -1

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isPlaying = intent.getBooleanExtra(Constants.STATUS_PLAYING, false)
            handlerActionMusic(intent.getSerializableExtra(Constants.ACTION_MUSIC) as Action)
            mSong = intent.getParcelableExtra<Song>(Constants.OBJECT_SONG) as Song
            handleLayoutMusic(intent.getSerializableExtra(Constants.ACTION_MUSIC) as Action)
        }
    }

    companion object {
        const val MESSAGE_MAIN = "MESSAGE_MAIN"
    }

    override fun initView() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(Constants.SEND_DATA_TO_ACTIVITY))
    }

    override fun onClickView() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fragmentMusic -> {
                    binding.bottomBar.isVisible = false
                    binding.bottomPlay.isVisible = false
                }

                R.id.splashFragment -> {
                    binding.bottomBar.isVisible = false
                    binding.bottomPlay.isVisible = false
                }

                else -> {
                    showBottomPlay()
                    binding.bottomBar.isVisible = true
                }
            }
        }

        val destinationId = navController.currentDestination?.id
        binding.bottomBar.selectedItem = { action ->
            when (action) {
                ActionBottomBar.LIBRARY -> {
                    if (destinationId != R.id.libraryFragment)
                        navController.navigate(R.id.libraryFragment)
                }

                ActionBottomBar.DISCOVER -> {
                    if (destinationId != R.id.homeFragment)
                        navController.navigate(R.id.homeFragment)
                }

                ActionBottomBar.ZINGCHART -> {
                    if (destinationId != R.id.zingchartFragment)
                        navController.navigate(R.id.zingchartFragment)
                }

                ActionBottomBar.RADIO -> {
                    if (destinationId != R.id.radioFragment)
                        navController.navigate(R.id.radioFragment)
                }

                ActionBottomBar.PROFILE -> {
                    if (destinationId != R.id.profileFragment)
                        navController.navigate(R.id.profileFragment)
                }
            }
        }

        binding.startMusic.setOnClickListener {
            clickStartService()
        }

        binding.play.setOnClickListener {
            sendActionToService(when {
                (binding.progressMusic.progress == binding.progressMusic.max && !isPlaying) -> Action.ACTION_START
                isPlaying -> Action.ACTION_PAUSE
                else -> Action.ACTION_RESUME
            })
        }

        binding.close.setOnClickListener {
            sendActionToService(Action.ACTION_CLEAR)
        }

        binding.bottomPlay.setOnClickListener {
            val options = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_up)
                .setExitAnim(R.anim.anim_normal)
                .setPopEnterAnim(R.anim.anim_normal)
                .setPopExitAnim(R.anim.slide_down)
                .build()
            val song = Data.listMusic()[indexSong]
            val bundle = Bundle().apply {
                putInt("id_music", song.idSong)
            }
            navController.navigate(R.id.fragmentMusic, bundle, options)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun clickStartService() {
        val intent = Intent(this, MusicService::class.java)
        val song = Data.listMusic()[0]
        intent.putExtra(MESSAGE_MAIN, song)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        timePlay.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun handleLayoutMusic(action: Action) {
        when (action) {
            Action.ACTION_START -> {
                showBottomPlay()
                showInfoSong()
                setStatusButtonPlay()
            }

            Action.ACTION_RESUME -> {
                setStatusButtonPlay()
            }

            Action.ACTION_PAUSE -> {
                setStatusButtonPlay()
            }

            else -> {
                binding.bottomPlay.visibility = View.GONE
            }
        }
    }

    private fun showBottomPlay() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomPlay.isVisible = navController.currentDestination?.id != R.id.fragmentMusic && indexSong != -1
    }

    @SuppressLint("SetTextI18n")
    private fun showInfoSong() {
        binding.avatar.setImageResource(mSong.avatar)
        binding.title.text = mSong.title
        binding.nameSingle.text = "Ca sĩ: ${mSong.nameSinger}"
        binding.progressMusic.max = mediaPlayer!!.duration
        binding.progressMusic.progress = 0
    }

    private fun setStatusButtonPlay() {
        binding.play.setImageResource(
            if (isPlaying) R.drawable.pause else R.drawable.play
        )
    }

    @SuppressLint("SetTextI18n")
    internal fun startServiceMusic(song: Song) {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(MESSAGE_MAIN, song)
        startService(intent)
    }

    /**
     * Send Action To Notification Music
     */
    private fun sendActionToService(action: Action) {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.RECEIVER_ACTION_MUSIC, action)
        startService(intent)
    }

    internal fun handlerActionMusic(action: Action) {
        when (action) {
            Action.ACTION_START -> startMusic()
            Action.ACTION_PAUSE -> pauseMusic()
            Action.ACTION_RESUME -> startMusic()
            Action.ACTION_NEXT -> nextSong()
            Action.ACTION_PREVIOUS -> previousSong()
            Action.ACTION_FINISH -> finishMusic()
            Action.ACTION_CLEAR -> clearMusic()
        }
    }

    private fun clearMusic() {
        isPlaying = false
        isFinish = false
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        binding.bottomPlay.visibility = View.GONE
        binding.bottomBar.isVisible = true
    }

    private fun finishMusic() {
        isFinish = false
        if (isRepeat) {
            Log.e("MainActivity", "Finish Music Repeat")
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } else {
            Log.e("MainActivity", "Finish Music Next")
            mediaPlayer?.isLooping = false
            handlerActionMusic(Action.ACTION_NEXT)
        }
    }

    private fun previousSong() {
        isPlaying = true
        if (indexSong > 0) {
            indexSong--
        } else {
            indexSong = Data.listMusic().size - 1
        }
        val currentFragment = getCurrentFragment()
        if (currentFragment is FragmentMusic) {
            currentFragment.initMusic(isNextBack = true)
        } else {
            resetMusic()
            val song = Data.listMusic()[indexSong]
            playMusic(song)
        }
    }

    private fun nextSong() {
        isPlaying = true
        if (indexSong < Data.listMusic().size - 1) {
            indexSong++
        } else {
            indexSong = 0
        }
        val currentFragment = getCurrentFragment()
        if (currentFragment is FragmentMusic) {
            currentFragment.initMusic(isNextBack = true)
        } else {
            resetMusic()
            val song = Data.listMusic()[indexSong]
            playMusic(song)
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        val currentFragment = getCurrentFragment()
        if (currentFragment is FragmentMusic) {
            currentFragment.pauseMusic()
        }
        sendActionToService(Action.ACTION_PAUSE)
    }

    private fun startMusic() {
        mediaPlayer?.start()
        isPlaying = true
        val currentFragment = getCurrentFragment()
        if (currentFragment is FragmentMusic) {
            currentFragment.startMusic()
        }
        sendActionToService(Action.ACTION_RESUME)
    }

    private fun setUpTimer() {
        timePlay.postDelayed(object : Runnable {
            override fun run() {
                if(dragToEnd) {
                    dragToEnd = false
                    handlerActionMusic(Action.ACTION_FINISH)
                }
                if (isPlaying) {
                    if (binding.progressMusic.progress == mediaPlayer!!.duration) {
                        dragToEnd = true
                    } else {
                        binding.progressMusic.progress = mediaPlayer!!.currentPosition
                        val currentFragment = getCurrentFragment()
                        if (currentFragment is FragmentMusic) {
                            currentFragment.updateProgressMusic(mediaPlayer!!.currentPosition)
                        }
                    }
                    mediaPlayer?.setOnCompletionListener {
                        isFinish = true
                    }
                }
                timePlay.postDelayed(this, 1000)
            }
        }, 0)
    }

    internal fun playMusic(song: Song) {
        startServiceMusic(song)
        mediaPlayer = MediaPlayer.create(this, song.sing)
        val currentFragment = getCurrentFragment()
        if (currentFragment is FragmentMusic) {
            currentFragment.setMaxProgress(mediaPlayer!!.duration)
            currentFragment.setTotalTime(mediaPlayer!!)
        }
        setUpTimer()
        handlerActionMusic(Action.ACTION_START)
    }

    internal fun resetMusic() {
        mediaPlayer?.reset()
    }

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivityMainBinding.inflate(inflater)

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        when (navController.currentDestination?.id) {
            R.id.homeFragment, R.id.libraryFragment, R.id.zingchartFragment, R.id.radioFragment, R.id.profileFragment -> {
                if (doubleBackToExitPressedOnce) {
                    this.finish()
                    return
                }

                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, "Nhấn thêm lần nữa để thoát", Toast.LENGTH_SHORT).show()

                // Đặt lại biến doubleBackToExitPressedOnce sau 2 giây
                Handler(Looper.getMainLooper()).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
            }
            R.id.splashFragment -> {}
            else -> super.onBackPressed()
        }
    }
}