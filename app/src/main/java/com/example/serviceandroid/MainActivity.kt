package com.example.serviceandroid

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.playback.PlaybackUiState
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.NetworkMonitor
import com.example.serviceandroid.utils.NetworkUiState
import com.example.serviceandroid.utils.getCurrentFragment
import com.example.serviceandroid.utils.moveTo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
@Suppress("DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var doubleBackToExitPressedOnce = false
    private val playbackViewModel by viewModels<PlaybackViewModel>()

    @Inject
    lateinit var songRepository: SongRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    /** Avoid mini-player work every playback tick (reduces layout jank in FragmentMusic). */
    private var lastMiniPlayerSongId: String? = null
    private var lastMiniPlayerSeekSyncedMs: Int = Int.MIN_VALUE
    private var lastMiniPlayerSeekSequence: Long = -1L
    private lateinit var miniPlayerSongAdapter: InformationSongAdapter
    private var lastNetworkBannerVisible = false
    private var bannerHideAnimation: Animation? = null
    private var networkBannerAllowed = false
    private var hasCompletedStartupOfflineDelay = false
    private var isStartupOfflineDelayScheduled = false
    private val offlineBannerStartupHandler = Handler(Looper.getMainLooper())
    private var offlineBannerStartupRunnable: Runnable? = null

    companion object {
        const val MESSAGE_MAIN = "MESSAGE_MAIN"
        private const val TAG = "NetworkBanner"
        private const val MINI_SEEK_UI_THROTTLE_MS = 200
        private const val OFFLINE_BANNER_STARTUP_DELAY_MS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOpenPlayerFromNotification(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenPlayerFromNotification(intent)
    }

    override fun onStart() {
        super.onStart()
        playbackViewModel.bind(this)
    }

    override fun initView() {
        updateNetworkBannerPosition()
        observeNetworkState()

        lifecycleScope.launch(Dispatchers.IO) {
            if (songRepository.getTopPlaylist().isEmpty()) {
                songRepository.refreshTopPlaylist()
            }
        }

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

        miniPlayerSongAdapter = InformationSongAdapter()
        miniPlayerSongAdapter.onClickView = {
            openMusicFromBottomPlay()
        }
        binding.viewPagerInfoSong.adapter = miniPlayerSongAdapter

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

    private fun updateNetworkBannerPosition() {
        val bannerRoot = binding.networkBanner.root
        val lp = bannerRoot.layoutParams as ConstraintLayout.LayoutParams
        val gap = resources.getDimensionPixelSize(R.dimen.network_banner_bottom_gap)
        lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToTop = if (binding.bottomPlay.visibility == View.VISIBLE) {
            R.id.bottomPlay
        } else {
            R.id.bottomBar
        }
        lp.bottomMargin = gap
        bannerRoot.layoutParams = lp
    }

    private fun observeNetworkState() {
        Log.d(TAG, "observeNetworkState: start collecting")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "observeNetworkState: lifecycle STARTED, collect active")
                networkMonitor.state.collect { uiState ->
                    applyNetworkBanner(uiState)
                }
            }
        }
    }

    private fun applyNetworkBanner(uiState: NetworkUiState) {
        if (!networkBannerAllowed) {
            cancelOfflineBannerStartupDelay()
            hideNetworkBannerImmediately()
            return
        }

        if (shouldDelayInitialOfflineBanner(uiState)) {
            if (!isStartupOfflineDelayScheduled) {
                isStartupOfflineDelayScheduled = true
                hideNetworkBannerViewOnly()
                offlineBannerStartupRunnable = Runnable {
                    offlineBannerStartupRunnable = null
                    isStartupOfflineDelayScheduled = false
                    hasCompletedStartupOfflineDelay = true
                    if (networkBannerAllowed) {
                        applyNetworkBannerImmediate(networkMonitor.state.value)
                    }
                }.also { runnable ->
                    offlineBannerStartupHandler.postDelayed(runnable, OFFLINE_BANNER_STARTUP_DELAY_MS)
                }
            }
            return
        }

        if (isStartupOfflineDelayScheduled && uiState.isOnline) {
            cancelOfflineBannerStartupDelay()
            hasCompletedStartupOfflineDelay = true
        }

        applyNetworkBannerImmediate(uiState)
    }

    private fun shouldDelayInitialOfflineBanner(uiState: NetworkUiState): Boolean {
        return !hasCompletedStartupOfflineDelay &&
            !uiState.isOnline &&
            uiState.showBanner
    }

    private fun cancelOfflineBannerStartupDelay() {
        offlineBannerStartupRunnable?.let { offlineBannerStartupHandler.removeCallbacks(it) }
        offlineBannerStartupRunnable = null
        isStartupOfflineDelayScheduled = false
    }

    private fun applyNetworkBannerImmediate(uiState: NetworkUiState) {
        val banner = binding.networkBanner
        val root = banner.root
        val content = banner.bannerContent

        if (uiState.showBanner) {
            bannerHideAnimation?.cancel()
            root.clearAnimation()

            val isOnline = uiState.isOnline
            content.setBackgroundResource(
                if (isOnline) R.drawable.bg_network_banner_online else R.drawable.bg_network_banner_offline,
            )
            content.invalidateOutline()
            banner.iconContainer.setBackgroundResource(
                if (isOnline) R.drawable.bg_network_icon_online else R.drawable.bg_network_icon_offline,
            )
            banner.imgIcon.setImageResource(
                if (isOnline) R.drawable.ic_wifi_connected else R.drawable.ic_wifi_off,
            )
            banner.tvMessage.text = uiState.message
            banner.tvSubtitle.text = getString(
                if (isOnline) R.string.network_online_subtitle else R.string.network_offline_subtitle,
            )
            updateNetworkBannerPosition()

            if (!lastNetworkBannerVisible) {
                root.isVisible = true
                root.startAnimation(AnimationUtils.loadAnimation(this, R.anim.network_banner_slide_in))
            }
        } else if (lastNetworkBannerVisible) {
            val slideOut = AnimationUtils.loadAnimation(this, R.anim.network_banner_slide_out)
            bannerHideAnimation = slideOut
            slideOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) = Unit

                override fun onAnimationEnd(animation: Animation?) {
                    if (!networkMonitor.state.value.showBanner) {
                        root.isVisible = false
                    }
                    bannerHideAnimation = null
                }

                override fun onAnimationRepeat(animation: Animation?) = Unit
            })
            root.startAnimation(slideOut)
        } else {
            root.isVisible = false
        }

        lastNetworkBannerVisible = uiState.showBanner
        Log.d(
            TAG,
            "applyNetworkBanner: isOnline=${uiState.isOnline} showBanner=${uiState.showBanner} " +
                "viewVisible=${root.isVisible} viewHeight=${root.height}",
        )
    }

    private fun hideNetworkBannerImmediately() {
        cancelOfflineBannerStartupDelay()
        hideNetworkBannerViewOnly()
    }

    private fun hideNetworkBannerViewOnly() {
        bannerHideAnimation?.cancel()
        binding.networkBanner.root.clearAnimation()
        binding.networkBanner.root.isVisible = false
        lastNetworkBannerVisible = false
    }

    private fun updateNetworkBannerAllowed(destinationId: Int) {
        val allowed = destinationId != R.id.splashFragment &&
            destinationId != R.id.fragmentMusic
        if (networkBannerAllowed == allowed) return
        networkBannerAllowed = allowed
        if (!allowed) {
            hideNetworkBannerImmediately()
        } else {
            applyNetworkBanner(networkMonitor.state.value)
        }
    }

    override fun onClickView() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNetworkBannerAllowed(destination.id)
            when (destination.id) {
                R.id.fragmentMusic -> {
                    binding.bottomBar.isVisible = false
                    binding.bottomPlay.visibility = View.GONE
                    updateNetworkBannerPosition()
                }

                R.id.splashFragment -> {
                    binding.bottomBar.isVisible = false
                    binding.bottomPlay.visibility = View.GONE
                    updateNetworkBannerPosition()
                }

                else -> {
                    applyBottomPlayVisibilityForDestination(destination.id)
                    binding.bottomBar.isVisible = true
                    updateNetworkBannerPosition()
                }
            }
        }

        updateNetworkBannerAllowed(navController.currentDestination?.id ?: R.id.splashFragment)

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

    private fun handleOpenPlayerFromNotification(intent: Intent?) {
        if (intent?.getBooleanExtra(Constants.EXTRA_OPEN_PLAYER_FROM_NOTIFICATION, false) != true) return
        val songId = intent.getStringExtra(Constants.EXTRA_NOTIFICATION_TARGET_SONG_ID).orEmpty()
        intent.removeExtra(Constants.EXTRA_OPEN_PLAYER_FROM_NOTIFICATION)
        intent.removeExtra(Constants.EXTRA_NOTIFICATION_TARGET_SONG_ID)
        val resolvedId = songId.takeIf { it.isNotBlank() }
            ?: playbackViewModel.playbackState.value.currentSong?.id
            ?: return
        binding.root.post {
            val navHost =
                supportFragmentManager.findFragmentById(R.id.navHostFragment) as? NavHostFragment
                    ?: return@post
            val navController = navHost.navController
            if (navController.currentDestination?.id == R.id.fragmentMusic) {
                // Đã ở màn player — chỉ mở lại app (task đã lên foreground); không navigate để tránh chồng FragmentMusic.
                return@post
            }
            navigateToFragmentMusic(resolvedId, preservePlaybackWhenOpening = true)
        }
    }

    private fun navigateToFragmentMusic(songId: String, preservePlaybackWhenOpening: Boolean) {
        val song = playbackViewModel.getPlaylist().find { it.id == songId }
            ?: playbackViewModel.playbackState.value.currentSong
            ?: return
        if (preservePlaybackWhenOpening) {
            playbackViewModel.setPendingOpenFromMiniPlayer()
        }
        val options = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_up)
            .setExitAnim(R.anim.anim_normal)
            .setPopEnterAnim(R.anim.anim_normal)
            .setPopExitAnim(R.anim.slide_down)
            .build()
        val bundle = Bundle().apply {
            putString("song_id", song.id)
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.fragmentMusic, bundle, options)
    }

    private fun openMusicFromBottomPlay() {
        val song = playbackViewModel.playbackState.value.currentSong ?: return
        navigateToFragmentMusic(song.id, preservePlaybackWhenOpening = true)
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
            if (song.id != lastMiniPlayerSongId) {
                lastMiniPlayerSongId = song.id
                com.bumptech.glide.Glide.with(this)
                    .load(song.thumbnailUrl)
                    .placeholder(R.drawable.ic_circle)
                    .error(R.drawable.ic_circle)
                    .into(binding.avatar)
                lastMiniPlayerSeekSyncedMs = Int.MIN_VALUE
            }
            syncMiniPlayerSongInfo(state)
        } ?: run {
            lastMiniPlayerSongId = null
            lastMiniPlayerSeekSyncedMs = Int.MIN_VALUE
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

    /**
     * Title/artist come from [InformationSongAdapter] (ViewPager pages).
     * Avatar comes from [PlaybackUiState.currentSong] directly.
     * When Home vs ZingChart refresh different playlists into [SongRepository],
     * the adapter must be re-synced or text at [queueIndex] no longer matches the playing track.
     */
    private fun syncMiniPlayerSongInfo(state: PlaybackUiState) {
        val song = state.currentSong ?: return
        val playlist = playbackViewModel.getPlaylist()
        if (playlist.isEmpty()) return

        val idx = state.queueIndex
        val adapterOutOfSync = miniPlayerSongAdapter.items.size != playlist.size ||
            idx !in miniPlayerSongAdapter.items.indices ||
            miniPlayerSongAdapter.items[idx].id != song.id

        if (adapterOutOfSync) {
            miniPlayerSongAdapter.items = playlist.toMutableList()
            miniPlayerSongAdapter.notifyDataSetChanged()
        }

        if (idx >= 0 && binding.viewPagerInfoSong.currentItem != idx) {
            binding.viewPagerInfoSong.setCurrentItem(idx, false)
        }
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
                View.GONE
            }
        updateNetworkBannerPosition()
    }

    override fun onDestroy() {
        cancelOfflineBannerStartupDelay()
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

            R.id.fragmentMusic -> {
                navController.popBackStack()
            }

            R.id.splashFragment -> {}
            else -> super.onBackPressed()
        }
    }
}
