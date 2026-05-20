package com.example.serviceandroid

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
import com.example.serviceandroid.databinding.ActivityMainBinding
import com.example.serviceandroid.fragment.music.FragmentMusic
import com.example.serviceandroid.playback.PlaybackUiState
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.getCurrentFragment
import com.example.serviceandroid.utils.moveTo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
@Suppress("DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var doubleBackToExitPressedOnce = false
    private val playbackViewModel by viewModels<PlaybackViewModel>()

    companion object {
        const val MESSAGE_MAIN = "MESSAGE_MAIN"
    }

    override fun onStart() {
        super.onStart()
        playbackViewModel.bind(this)
    }

    override fun onStop() {
        playbackViewModel.unbind(this)
        super.onStop()
    }

    override fun initView() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playbackViewModel.playbackState.collect { state ->
                    applyPlaybackUi(state)
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

        state.currentSong?.let { song ->
            binding.avatar.setImageResource(song.avatar)
        }

        if (state.queueIndex >= 0) {
            binding.viewPagerInfoSong.setCurrentItem(state.queueIndex, false)
        }

        binding.progressMusic.max = state.durationMs.coerceAtLeast(0)
        if (state.durationMs > 0) {
            binding.progressMusic.progress =
                state.positionMs.coerceIn(0, state.durationMs)
        }
        binding.play.setImageResource(
            if (state.isPlaying) R.drawable.pause else R.drawable.play
        )

        (getCurrentFragment() as? FragmentMusic)?.onPlaybackStateChanged(state)
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
