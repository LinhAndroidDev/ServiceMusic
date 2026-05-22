package com.example.serviceandroid

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.example.serviceandroid.adapter.InformationSongAdapter
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.custom.ActionBottomBar
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.ActivityMainBinding
import com.example.serviceandroid.fragment.music.FragmentMusic
import com.example.serviceandroid.playback.PlaybackUiState
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.getCurrentFragment
import com.example.serviceandroid.utils.moveTo
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlinx.coroutines.launch

@AndroidEntryPoint
@Suppress("DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var doubleBackToExitPressedOnce = false
    private val playbackViewModel by viewModels<PlaybackViewModel>()

    /** Avoid mini-player work every playback tick (reduces layout jank in FragmentMusic). */
    private var lastMiniPlayerSongId: Int = -1
    private var lastMiniPlayerSeekSyncedMs: Int = Int.MIN_VALUE
    private var lastMiniPlayerSeekSequence: Long = -1L

    companion object {
        const val MESSAGE_MAIN = "MESSAGE_MAIN"
        private const val MINI_SEEK_UI_THROTTLE_MS = 200
    }

    override fun onStart() {
        super.onStart()
        playbackViewModel.bind(this)
    }

    override fun initView() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playbackViewModel.playbackState.collect { state ->
                        applyPlaybackUi(state)
                    }
                }
                launch {
                    playbackViewModel.miniPlayerIsFavourite.collect { favourite ->
                        applyMiniPlayerFavouriteIcon(favourite)
                    }
                }
            }
        }

        val adapterInfoSong = InformationSongAdapter()
        adapterInfoSong.items = playbackViewModel.getPlaylist()
        adapterInfoSong.onClickView = {
            openMusicFromBottomPlay()
        }
        binding.viewPagerInfoSong.adapter = adapterInfoSong

        binding.viewPagerInfoSong.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val idx = playbackViewModel.playbackState.value.queueIndex
                if (idx != position && idx != -1) {
                    playbackViewModel.playSongAtIndex(this@MainActivity, position)
                }
            }
        })
    }

    override fun onClickView() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fragmentMusic -> {
                    binding.bottomBar.isVisible = false
                    binding.bottomPlay.visibility = View.INVISIBLE
                }

                R.id.splashFragment -> {
                    binding.bottomBar.isVisible = false
                    binding.bottomPlay.visibility = View.INVISIBLE
                }

                else -> {
                    applyBottomPlayVisibilityForDestination(destination.id)
                    binding.bottomBar.isVisible = true
                }
            }
        }

        binding.bottomBar.selectedItem = { action ->
            when (action) {
                ActionBottomBar.LIBRARY -> navController.moveTo(R.id.libraryFragment)
                ActionBottomBar.DISCOVER -> navController.moveTo(R.id.homeFragment)
                ActionBottomBar.ZINGCHART -> navController.moveTo(R.id.zingchartFragment)
                ActionBottomBar.RADIO -> navController.moveTo(R.id.radioFragment)
                ActionBottomBar.PROFILE -> navController.moveTo(R.id.profileFragment)
            }
        }

        binding.startMusic.setOnClickListener {
            playbackViewModel.playFirstSong(this)
        }

        binding.play.setOnClickListener {
            val st = playbackViewModel.playbackState.value
            playbackViewModel.toggleBottomPlayPause(
                this,
                st.positionMs,
                st.durationMs,
                st.isPlaying
            )
        }

        binding.close.setOnClickListener {
            playbackViewModel.clear(this)
        }

        binding.bottomPlay.setOnClickListener {
            openMusicFromBottomPlay()
        }

        binding.favourite.setOnClickListener {
            val song = playbackViewModel.playbackState.value.currentSong ?: return@setOnClickListener
            if (playbackViewModel.miniPlayerIsFavourite.value) {
                DialogConfirm().apply {
                    title = song.title
                    onClickRemove = {
                        playbackViewModel.toggleCurrentSongFavourite(song) { stillFavourite ->
                            if (!stillFavourite) {
                                Toast.makeText(
                                    this@MainActivity,
                                    this@MainActivity.getString(R.string.toast_removed_favourite),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }.show(supportFragmentManager, null)
            } else {
                playbackViewModel.toggleCurrentSongFavourite(song) { added ->
                    if (added) {
                        Toast.makeText(
                            this,
                            getString(R.string.toast_added_favourite),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun openMusicFromBottomPlay() {
        val song = playbackViewModel.playbackState.value.currentSong ?: return
        playbackViewModel.setPendingOpenFromMiniPlayer()
        val options = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_up)
            .setExitAnim(R.anim.anim_normal)
            .setPopEnterAnim(R.anim.anim_normal)
            .setPopExitAnim(R.anim.slide_down)
            .build()
        val bundle = Bundle().apply {
            putInt("id_music", song.idSong)
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.fragmentMusic, bundle, options)
    }

    private fun applyPlaybackUi(state: PlaybackUiState) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val destId = navController.currentDestination?.id

        if (destId != R.id.fragmentMusic && destId != R.id.splashFragment) {
            applyBottomPlayVisibilityForDestination(destId ?: 0)
        }

        if (state.seekSequence != lastMiniPlayerSeekSequence) {
            lastMiniPlayerSeekSequence = state.seekSequence
            lastMiniPlayerSeekSyncedMs = Int.MIN_VALUE
        }

        state.currentSong?.let { song ->
            if (song.idSong != lastMiniPlayerSongId) {
                lastMiniPlayerSongId = song.idSong
                binding.avatar.setImageResource(song.avatar)
                lastMiniPlayerSeekSyncedMs = Int.MIN_VALUE
            }
        } ?: run {
            lastMiniPlayerSongId = -1
            lastMiniPlayerSeekSyncedMs = Int.MIN_VALUE
        }

        if (state.queueIndex >= 0 && binding.viewPagerInfoSong.currentItem != state.queueIndex) {
            binding.viewPagerInfoSong.setCurrentItem(state.queueIndex, false)
        }

        val dur = state.durationMs.coerceAtLeast(0)
        binding.progressMusic.max = dur
        if (dur > 0) {
            val pos = state.positionMs.coerceIn(0, dur)
            val forceSeek =
                !state.isPlaying ||
                    lastMiniPlayerSeekSyncedMs == Int.MIN_VALUE ||
                    abs(pos - lastMiniPlayerSeekSyncedMs) >= MINI_SEEK_UI_THROTTLE_MS
            if (forceSeek) {
                lastMiniPlayerSeekSyncedMs = pos
                binding.progressMusic.progress = pos
            }
        }
        binding.play.setImageResource(
            if (state.isPlaying) R.drawable.pause else R.drawable.play
        )

        (getCurrentFragment() as? FragmentMusic)?.onPlaybackStateChanged(state)
    }

    private fun applyMiniPlayerFavouriteIcon(favourite: Boolean) {
        if (favourite) {
            binding.favourite.setImageResource(R.drawable.ic_favourite_fill)
            binding.favourite.imageTintList =
                ColorStateList.valueOf(getColor(R.color.red))
        } else {
            binding.favourite.setImageResource(R.drawable.ic_favourite_thin)
            binding.favourite.imageTintList =
                ColorStateList.valueOf(getColor(R.color.white))
        }
    }

    private fun applyBottomPlayVisibilityForDestination(destinationId: Int) {
        val st = playbackViewModel.playbackState.value
        binding.bottomPlay.visibility =
            if (destinationId != R.id.fragmentMusic && st.hasActivePlayer) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) {
            playbackViewModel.unbind(this)
            val intent = android.content.Intent(this, com.example.serviceandroid.service.MusicService::class.java)
            stopService(intent)
        }
        super.onDestroy()
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

                Handler(Looper.getMainLooper()).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
            }

            R.id.splashFragment -> {}
            else -> super.onBackPressed()
        }
    }
}
