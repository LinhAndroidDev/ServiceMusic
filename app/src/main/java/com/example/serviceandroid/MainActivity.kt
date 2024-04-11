package com.example.serviceandroid

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.custom.ActionBottomBar
import com.example.serviceandroid.databinding.ActivityMainBinding
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.service.HelloService
import java.util.Timer
import java.util.TimerTask


@Suppress("DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private lateinit var mSong: Song
    private var isPlaying: Boolean = false
    private var timer: Timer? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mSong = intent.getParcelableExtra<Song>(Constants.OBJECT_SONG) as Song
            isPlaying = intent.getBooleanExtra(Constants.STATUS_PLAYING, false)
            handleLayoutMusic(intent.getSerializableExtra(Constants.ACTION_MUSIC) as Action)
        }
    }

    companion object {
        const val MESSAGE_MAIN = "MESSAGE_MAIN"
        const val INDEX_MUSIC = "INDEX_MUSIC"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(Constants.SEND_DATA_TO_ACTIVITY))

        onClickView()
    }

    private fun onClickView() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

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
    }

    internal fun visibleBottomBar() {
        binding.bottomBar.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun clickStartService() {
        val intent = Intent(this, HelloService::class.java)
        val song = Data.listMusic()[0]
        intent.putExtra(MESSAGE_MAIN, song)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun handleLayoutMusic(action: Action) {
        when (action) {
            Action.ACTION_START -> {
                binding.bottomPlay.visibility = View.VISIBLE
                showInfoSong()
                setTimerPlay()
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

    private fun setTimerPlay() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isPlaying) {
                    binding.progressMusic.progress += 1
                }
            }
        }, 0, 1000)
    }

    @SuppressLint("SetTextI18n")
    private fun showInfoSong() {
        binding.avatar.setImageResource(mSong.avatar)
        binding.title.text = mSong.title
        binding.nameSingle.text = "Ca sĩ: ${mSong.nameSinger}"
        binding.bottomPlay.visibility = View.VISIBLE
        binding.progressMusic.max = mSong.time
        binding.progressMusic.progress = 0
    }

    private fun setStatusButtonPlay() {
        binding.play.setImageResource(
            if (isPlaying) R.drawable.pause else R.drawable.play
        )
    }

    private fun sendActionToService(action: Action) {
        val intent = Intent(this, HelloService::class.java)
        intent.putExtra(Constants.RECEIVER_ACTION_MUSIC, action)
        startService(intent)
    }

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivityMainBinding.inflate(inflater)

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        when (navController.currentDestination?.id) {
            R.id.homeFragment -> this.finish()
            R.id.splashFragment -> {}
            else -> super.onBackPressed()
        }
    }
}