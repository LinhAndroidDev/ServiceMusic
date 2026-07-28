package com.example.serviceandroid.fragment.music

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.FragmentMusicBinding
import com.example.serviceandroid.databinding.ItemMusicPlayerPageBinding
import com.example.serviceandroid.databinding.ItemMusicSingerPageBinding
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.model.Singer
import com.google.android.material.tabs.TabLayout
import com.example.serviceandroid.lyrics.LineLyricsAdapter
import com.example.serviceandroid.lyrics.SongLyricsLoader
import com.example.serviceandroid.lyrics.TimedLyricLine
import com.example.serviceandroid.model.Repeat
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.playback.PlaybackUiState
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.CustomAnimator
import com.example.serviceandroid.utils.DateUtils
import com.example.serviceandroid.utils.SwipeDownDismissHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

@AndroidEntryPoint
class FragmentMusic : BaseFragment<FragmentMusicBinding>() {

    private val args: FragmentMusicArgs by navArgs()
    private val viewModel by viewModels<FragmentMusicViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private val fadeIn by lazy { AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_fade_in) }
    private var isFavourite: Boolean = false
    private var lastRenderedSongId: String? = null

    private lateinit var pagerAdapter: MusicNowPlayingPagerAdapter
    private var playerPageBinding: ItemMusicPlayerPageBinding? = null
    private var playerControlsAttached: Boolean = false

    private var lyricLines: List<TimedLyricLine>? = null
    private var lineLyricsAdapter: LineLyricsAdapter? = null
    /** Last active line index from [activeLineIndexAt]; [Int.MIN_VALUE] = not yet synced. */
    private var lastActiveLineIndex: Int = Int.MIN_VALUE
    /** Coalesces SeekBar/time label updates while playing to avoid layout thrash vs. cover rotation. */
    private var lastSeekUiSyncedMs: Int = Int.MIN_VALUE
    private var lastDurationLabelMs: Int = -1
    /** Last anchor used for lyric list scroll (activeLine - 1); avoids restarting identical smooth scrolls. */
    private var lastLyricsScrollAnchor: Int = Int.MIN_VALUE
    /** When [PlaybackUiState.seekSequence] changes, SeekBar throttling is bypassed so bars jump to seek target. */
    private var lastPlaybackSeekSequence: Long = -1L
    private var pendingInitSongId: String? = null
    /** True while user drags the seek bar — avoids spamming MediaPlayer.seekTo during scrub. */
    private var isUserSeeking: Boolean = false
    private var singerTabListener: TabLayout.OnTabSelectedListener? = null
    private var isBindingSingerTabs: Boolean = false
    private var lastSingerTabSongId: String? = null
    private var swipeDownDismissHelper: SwipeDownDismissHelper? = null

    private val playerPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            updatePagerIndicatorProgress(position, positionOffset)
        }

        override fun onPageSelected(position: Int) {
            if (position == PAGE_LYRICS) {
                updateLineLyricsPlayback(playbackViewModel.playbackState.value.positionMs, force = true)
            }
            updatePagerIndicatorProgress(position, 0f)
        }
    }

    private companion object {
        private const val PAGE_SINGER = 0
        private const val PAGE_SONG = 1
        private const val PAGE_LYRICS = 2
        private const val INDICATOR_WIDTH_NORMAL_DP = 12f
        private const val INDICATOR_WIDTH_SELECTED_DP = 18f
        private const val INDICATOR_HEIGHT_NORMAL_DP = 2f
        private const val INDICATOR_HEIGHT_SELECTED_DP = 2.5f
        private const val INDICATOR_RADIUS_NORMAL_DP = 1f
        private const val INDICATOR_RADIUS_SELECTED_DP = 2f
        private const val INDICATOR_ALPHA_NORMAL = 0.45f
        private const val INDICATOR_ALPHA_SELECTED = 1f
        private const val LYRIC_TIME_EPS = 1e-4
        /** Min ms between SeekBar / clock UI updates while playing (lyrics still use full [positionMs]). */
        private const val SEEK_UI_THROTTLE_MS = 220
    }

    override fun getFragmentBinding(inflater: LayoutInflater): FragmentMusicBinding {
        return FragmentMusicBinding.inflate(inflater)
    }

    override fun initView() {
        val songId = args.songId.ifBlank {
            arguments?.getString("song_id").orEmpty()
        }
        pendingInitSongId = songId.takeIf { it.isNotBlank() }

        pagerAdapter = MusicNowPlayingPagerAdapter(
            onSingerPageBound = { spb ->
                setupSingerTabListener(spb)
                bindSingerUi(viewModel.singerUiState.value)
            },
            onPlayerPageBound = { pb ->
                playerPageBinding = pb
                if (!playerControlsAttached) {
                    playerControlsAttached = true
                    CustomAnimator.rotationImage(pb.imgSong)
                    setupPlayerPageInteractions(pb)
                    pendingInitSongId?.let {
                        initMusic(it)
                        pendingInitSongId = null
                    }
                }
            },
        )
        binding.playerPager.adapter = pagerAdapter
        binding.playerPager.setCurrentItem(PAGE_SONG, false)
        binding.playerPager.registerOnPageChangeCallback(playerPagerCallback)
        @Suppress("DEPRECATION")
        binding.playerPager.offscreenPageLimit = 2
        setupPagerIndicatorDrawables()
        updatePagerIndicatorProgress(PAGE_SONG, 0f)

        binding.playerTransport.imgRepeat.setImageResource(viewModel.getTypeRepeat().value)
        setupTransportControls()
        setupSwipeDownDismiss()

        observeSingerUiState()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavourite.collect {
                    isFavourite = it
                    playerPageBinding?.let { pb ->
                        if (it) {
                            pb.imgFavourite.setImageResource(R.drawable.ic_favourite_fill)
                            pb.imgFavourite.imageTintList =
                                ColorStateList.valueOf(requireContext().getColor(R.color.red))
                        } else {
                            pb.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
                            pb.imgFavourite.imageTintList =
                                ColorStateList.valueOf(requireContext().getColor(R.color.white))
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.imageCover.setRenderEffect(
                RenderEffect.createBlurEffect(
                    50.0f, 50.0f, Shader.TileMode.CLAMP
                )
            )
        }
    }

    private fun setupPagerIndicatorDrawables() {
        val color = requireContext().getColor(R.color.white)
        listOf(binding.indicatorSinger, binding.indicatorSong, binding.indicatorLyrics).forEach { view ->
            view.background = GradientDrawable().apply { setColor(color) }
        }
    }

    private fun updatePagerIndicatorProgress(pagePosition: Int, pageOffset: Float) {
        applyIndicatorFraction(binding.indicatorSinger, indicatorSelectionFraction(PAGE_SINGER, pagePosition, pageOffset))
        applyIndicatorFraction(binding.indicatorSong, indicatorSelectionFraction(PAGE_SONG, pagePosition, pageOffset))
        applyIndicatorFraction(binding.indicatorLyrics, indicatorSelectionFraction(PAGE_LYRICS, pagePosition, pageOffset))
    }

    private fun indicatorSelectionFraction(
        indicatorIndex: Int,
        pagePosition: Int,
        pageOffset: Float,
    ): Float {
        val diff = indicatorIndex - pagePosition
        return when (diff) {
            0 -> 1f - pageOffset
            1 -> pageOffset
            else -> 0f
        }.coerceIn(0f, 1f)
    }

    private fun applyIndicatorFraction(view: View, fraction: Float) {
        val density = resources.displayMetrics.density
        val widthPx = lerp(INDICATOR_WIDTH_NORMAL_DP, INDICATOR_WIDTH_SELECTED_DP, fraction, density)
        val heightPx = lerp(INDICATOR_HEIGHT_NORMAL_DP, INDICATOR_HEIGHT_SELECTED_DP, fraction, density)
        val radiusPx = lerp(INDICATOR_RADIUS_NORMAL_DP, INDICATOR_RADIUS_SELECTED_DP, fraction, density)

        view.layoutParams = view.layoutParams.apply {
            width = widthPx.toInt().coerceAtLeast(1)
            height = heightPx.toInt().coerceAtLeast(1)
        }
        view.alpha = INDICATOR_ALPHA_NORMAL + (INDICATOR_ALPHA_SELECTED - INDICATOR_ALPHA_NORMAL) * fraction
        (view.background as? GradientDrawable)?.cornerRadius = radiusPx
    }

    private fun lerp(startDp: Float, endDp: Float, fraction: Float, density: Float): Float =
        (startDp + (endDp - startDp) * fraction) * density

    private fun handleRepeat() {
        val transport = binding.playerTransport
        val repeat = viewModel.getTypeRepeat()
        when (repeat) {
            Repeat.NOT_REPEAT -> {
                viewModel.saveTypeRepeat(Repeat.REPEAT_ALL)
                transport.imgRepeat.setImageResource(R.drawable.ic_repeat_all)
            }
            Repeat.REPEAT_ALL -> {
                viewModel.saveTypeRepeat(Repeat.REPEAT_ONE)
                transport.imgRepeat.setImageResource(R.drawable.ic_repeat_one)
            }
            Repeat.REPEAT_ONE -> {
                viewModel.saveTypeRepeat(Repeat.NOT_REPEAT)
                transport.imgRepeat.setImageResource(R.drawable.ic_not_repeat)
            }
        }
        playbackViewModel.syncRepeatMode(requireContext())
    }

    override fun onClickView() {
        binding.backMusic.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSwipeDownDismiss() {
        swipeDownDismissHelper = SwipeDownDismissHelper(
            handleView = binding.swipeDismissHandle,
            contentView = binding.root,
            onDismiss = {
                if (isAdded) {
                    binding.root.isInvisible = true
                    findNavController().popBackStack()
                }
            },
        )
    }

    private fun setupTransportControls() {
        val transport = binding.playerTransport

        transport.imgNext.setOnClickListener {
            CustomAnimator.animateTransportButton(transport.imgNext) {
                playbackViewModel.next(requireContext())
            }
        }
        transport.imgPrevious.setOnClickListener {
            CustomAnimator.animateTransportButton(transport.imgPrevious) {
                playbackViewModel.previous(requireContext())
            }
        }

        transport.imgPlay.setOnClickListener {
            CustomAnimator.animateTransportButton(transport.imgPlay) {
                val st = playbackViewModel.playbackState.value
                if (!st.isPlaying) {
                    playbackViewModel.resume(requireContext())
                } else {
                    playbackViewModel.pause(requireContext())
                }
            }
        }

        transport.imgRepeat.setOnClickListener {
            handleRepeat()
        }

        transport.progressMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Preview time only while dragging; actual seek on release avoids stream re-buffer glitches.
                    setProgressTime(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                val progress = seekBar?.progress ?: return
                playbackViewModel.seekTo(requireContext(), progress)
                setProgressTime(progress)
            }
        })
    }

    private fun setupPlayerPageInteractions(pb: ItemMusicPlayerPageBinding) {
        pb.imgFavourite.setOnClickListener {
            val song = playbackViewModel.playbackState.value.currentSong ?: return@setOnClickListener
            if (!isFavourite) {
                viewModel.insertSong(song, DateUtils.getTimeCurrent()) {
                    if (!isAdded) return@insertSong
                    playbackViewModel.refreshMiniPlayerFavouriteForCurrentSong()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_added_favourite),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                DialogConfirm().apply {
                    title = song.title
                    onClickRemove = {
                        viewModel.deleteSongById(song.id) {
                            if (!this@FragmentMusic.isAdded) return@deleteSongById
                            playbackViewModel.refreshMiniPlayerFavouriteForCurrentSong()
                            Toast.makeText(
                                this@FragmentMusic.requireContext(),
                                this@FragmentMusic.getString(R.string.toast_removed_favourite),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.show(parentFragmentManager, "")
            }
        }
    }

    private fun initMusic(songId: String) {
        if (playerPageBinding == null) return
        resetFavourite()
        val resolvedIndex = playbackViewModel.resolveQueueIndexForSongId(songId)
        val song = playbackViewModel.getPlaylist()[resolvedIndex]
        val fromMini = playbackViewModel.consumePendingOpenFromMiniPlayer()
        val st = playbackViewModel.playbackState.value
        val keepPlaying = fromMini &&
            st.queueIndex == resolvedIndex &&
            st.hasActivePlayer

        if (!keepPlaying) {
            playbackViewModel.playSongAtIndex(requireContext(), resolvedIndex)
        }

        bindSongMetadata(song)
        viewModel.checkSongById(song.id)
    }

    private fun bindSongMetadata(song: Song) {
        val pb = playerPageBinding ?: return
        lastRenderedSongId = song.id
        lastActiveLineIndex = Int.MIN_VALUE
        lastSeekUiSyncedMs = Int.MIN_VALUE
        lastDurationLabelMs = -1
        lastLyricsScrollAnchor = Int.MIN_VALUE
        lastPlaybackSeekSequence = -1L
        Glide.with(this)
            .load(song.thumbnailUrl)
            .error(R.drawable.ic_circle)
            .placeholder(R.drawable.ic_circle)
            .into(pb.imgSong)
        Glide.with(this)
            .load(song.thumbnailUrl)
            .error(R.drawable.ic_circle)
            .placeholder(R.drawable.ic_circle)
            .into(binding.imageCover)
        pb.imgSong.startAnimation(fadeIn)
        pb.tvNameSong.text = song.title
        pb.tvNameSinger.text = song.nameSinger
        val st = playbackViewModel.playbackState.value
        val duration = st.durationMs
        if (duration > 0) {
            setMaxProgress(duration)
            setTotalTimeFromDuration(duration)
            lastDurationLabelMs = duration
            val pos = st.positionMs.coerceIn(0, duration)
            binding.playerTransport.progressMusic.progress = pos
            setProgressTime(pos)
            lastSeekUiSyncedMs = pos
        }
        if (st.isPlaying) startMusic() else pauseMusic()

        viewModel.loadSingersForSong(song.id, song.nameSinger)
        loadLyricsForSong(song)
    }

    private fun observeSingerUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.singerUiState.collect { state ->
                    bindSingerUi(state)
                }
            }
        }
    }

    private fun setupSingerTabListener(spb: ItemMusicSingerPageBinding) {
        if (singerTabListener != null) return
        singerTabListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (isBindingSingerTabs) return
                viewModel.selectSingerTab(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        }.also { spb.tabSingers.addOnTabSelectedListener(it) }
    }

    private fun bindSingerUi(state: SingerUiState) {
        val spb = pagerAdapter.singerPageBinding ?: return
        if (state.songId.isNotBlank() && state.songId != lastRenderedSongId) return

        when {
            state.isLoading -> {
                spb.progressSinger.visibility = View.VISIBLE
                spb.tvSingerEmpty.visibility = View.GONE
                spb.singerContent.visibility = View.GONE
            }
            state.singers.isEmpty() -> {
                spb.progressSinger.visibility = View.GONE
                spb.tvSingerEmpty.visibility = View.VISIBLE
                spb.singerContent.visibility = View.GONE
            }
            else -> {
                spb.progressSinger.visibility = View.GONE
                spb.tvSingerEmpty.visibility = View.GONE
                spb.singerContent.visibility = View.VISIBLE
                setupSingerTabs(spb, state)
                state.selectedSinger?.let { bindSelectedSinger(spb, it) }
            }
        }
    }

    private fun setupSingerTabs(spb: ItemMusicSingerPageBinding, state: SingerUiState) {
        val tabLayout = spb.tabSingers
        if (state.singers.size > 1) {
            tabLayout.visibility = View.VISIBLE
            spb.tvSingerName.visibility = View.GONE
            val needsRebuild = lastSingerTabSongId != state.songId ||
                tabLayout.tabCount != state.singers.size
            if (needsRebuild) {
                lastSingerTabSongId = state.songId
                isBindingSingerTabs = true
                tabLayout.removeAllTabs()
                state.singers.forEach { singer ->
                    tabLayout.addTab(tabLayout.newTab().setText(singer.name))
                }
                tabLayout.getTabAt(state.selectedIndex.coerceIn(0, state.singers.lastIndex))?.select()
                isBindingSingerTabs = false
            } else if (tabLayout.selectedTabPosition != state.selectedIndex) {
                isBindingSingerTabs = true
                tabLayout.getTabAt(state.selectedIndex)?.select()
                isBindingSingerTabs = false
            }
        } else {
            tabLayout.visibility = View.GONE
            spb.tvSingerName.visibility = View.VISIBLE
            lastSingerTabSongId = state.songId
        }
    }

    private fun bindSelectedSinger(spb: ItemMusicSingerPageBinding, singer: Singer) {
        if (spb.tabSingers.visibility != View.VISIBLE) {
            spb.tvSingerName.text = singer.name
        }
        Glide.with(this)
            .load(singer.avatarUrl.takeIf { it.isNotBlank() })
            .error(R.drawable.ic_circle)
            .placeholder(R.drawable.ic_circle)
            .into(spb.imgSingerAvatar)
        spb.tvSingerBio.text = singer.description.takeIf { it.isNotBlank() }
            ?: getString(R.string.singer_bio_empty)
    }

    private fun ensureLineLyricsAdapter(): LineLyricsAdapter {
        lineLyricsAdapter?.let { existing ->
            existing.onLineClickListener = { line -> seekPlaybackToLyricLine(line) }
            return existing
        }
        val adapter = LineLyricsAdapter(
            requireContext().getColor(R.color.text_white),
            requireContext().getColor(R.color.lyric_line_active),
        )
        adapter.onLineClickListener = { line -> seekPlaybackToLyricLine(line) }
        lineLyricsAdapter = adapter
        return adapter
    }

    /** Seek to the timestamp of this lyric line (ms from [TimedLyricLine.startSec]). */
    private fun seekPlaybackToLyricLine(line: TimedLyricLine) {
        val st = playbackViewModel.playbackState.value
        if (!st.hasActivePlayer) return
        val ms = (line.startSec * 1000.0).roundToInt().coerceAtLeast(0)
        val dur = st.durationMs
        val clamped = if (dur > 0) ms.coerceIn(0, dur) else ms
        playbackViewModel.seekTo(requireContext(), clamped)
    }

    private fun activeLineIndexAt(lines: List<TimedLyricLine>, positionSec: Double): Int {
        if (lines.isEmpty()) return -1
        val t = positionSec + LYRIC_TIME_EPS
        if (t < lines[0].startSec) return -1
        var last = -1
        for (i in lines.indices) {
            if (lines[i].startSec <= t) last = i
        }
        return last
    }

    private fun attachLyricsRecyclerAdapterIfNeeded() {
        val rv = pagerAdapter.lyricsRecycler ?: return
        val lines = lyricLines ?: return
        if (lines.isEmpty()) return
        val adapter = ensureLineLyricsAdapter()
        if (rv.adapter !== adapter) {
            rv.adapter = adapter
            adapter.submitLines(lines)
        }
    }

    private fun loadLyricsForSong(song: Song) {
        if (pagerAdapter.lyricsRecycler == null || pagerAdapter.lyricsEmpty == null) {
            binding.playerPager.post { loadLyricsForSong(song) }
            return
        }
        val rv = pagerAdapter.lyricsRecycler!!
        val empty = pagerAdapter.lyricsEmpty!!
        lyricLines = null
        lastActiveLineIndex = Int.MIN_VALUE
        rv.adapter = null
        val targetSongId = song.id
        viewLifecycleOwner.lifecycleScope.launch {
            val lines = withContext(Dispatchers.IO) {
                SongLyricsLoader.loadTimedLines(song)
            }
            if (!isAdded) return@launch
            if (lastRenderedSongId != targetSongId) return@launch
            if (lines.isNullOrEmpty()) {
                lyricLines = null
                rv.visibility = android.view.View.GONE
                empty.visibility = android.view.View.VISIBLE
            } else {
                lyricLines = lines
                empty.visibility = android.view.View.GONE
                rv.visibility = android.view.View.VISIBLE
                val adapter = ensureLineLyricsAdapter()
                rv.adapter = adapter
                adapter.submitLines(lines)
                lastActiveLineIndex = Int.MIN_VALUE
                lastLyricsScrollAnchor = Int.MIN_VALUE
                updateLineLyricsPlayback(playbackViewModel.playbackState.value.positionMs, force = true)
            }
        }
    }

    private fun updateLineLyricsPlayback(positionMs: Int, force: Boolean = false) {
        val lines = lyricLines ?: return
        if (lines.isEmpty()) return
        val rv = pagerAdapter.lyricsRecycler ?: return
        attachLyricsRecyclerAdapterIfNeeded()
        val t = positionMs / 1000.0
        val active = activeLineIndexAt(lines, t)
        if (!force && active == lastActiveLineIndex) return
        lastActiveLineIndex = active
        val adapter = ensureLineLyricsAdapter()
        adapter.setActiveLine(active)
        if (active >= 0 && binding.playerPager.currentItem == PAGE_LYRICS) {
            val anchor = (active - 1).coerceAtLeast(0)
            if (force || anchor != lastLyricsScrollAnchor) {
                lastLyricsScrollAnchor = anchor
                smoothScrollLyricsAnchorToTop(rv, anchor)
            }
        }
    }

    /**
     * Scrolls so [anchorPosition] is the first visible row; the active line (anchor + 1) sits
     * as the second row from the top (one context line above), when it exists.
     */
    private fun smoothScrollLyricsAnchorToTop(rv: RecyclerView, anchorPosition: Int) {
        val lm = rv.layoutManager as? LinearLayoutManager ?: return
        val scroller = object : LinearSmoothScroller(rv.context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
        }
        scroller.targetPosition = anchorPosition
        lm.startSmoothScroll(scroller)
    }

    fun onPlaybackStateChanged(state: PlaybackUiState) {
        if (state.seekSequence != lastPlaybackSeekSequence) {
            lastPlaybackSeekSequence = state.seekSequence
            lastSeekUiSyncedMs = Int.MIN_VALUE
        }
        val song = state.currentSong ?: return
        if (song.id != lastRenderedSongId) {
            bindSongMetadata(song)
            viewModel.checkSongById(song.id)
        }
        val transport = binding.playerTransport
        if (state.durationMs > 0) {
            val pos = state.positionMs.coerceIn(0, state.durationMs)
            if (transport.progressMusic.max != state.durationMs) {
                transport.progressMusic.max = state.durationMs
                lastSeekUiSyncedMs = Int.MIN_VALUE
            }
            val forceSeekUi = !isUserSeeking && (
                !state.isPlaying ||
                    lastSeekUiSyncedMs == Int.MIN_VALUE ||
                    kotlin.math.abs(pos - lastSeekUiSyncedMs) >= SEEK_UI_THROTTLE_MS
                )
            if (forceSeekUi) {
                lastSeekUiSyncedMs = pos
                transport.progressMusic.progress = pos
                setProgressTime(pos)
            }
            if (state.durationMs != lastDurationLabelMs) {
                lastDurationLabelMs = state.durationMs
                setTotalTimeFromDuration(state.durationMs)
            }
        }
        if (state.isPlaying) startMusic() else pauseMusic()
        updateLineLyricsPlayback(state.positionMs)
    }

    @SuppressLint("SimpleDateFormat")
    private fun setProgressTime(currentPosition: Int) {
        binding.playerTransport.tvProgressTime.text =
            SimpleDateFormat(Constants.MINUTES).format(currentPosition)
    }

    @SuppressLint("SimpleDateFormat")
    private fun setTotalTimeFromDuration(durationMs: Int) {
        binding.playerTransport.tvTotalTime.text =
            SimpleDateFormat(Constants.MINUTES).format(durationMs)
    }

    private fun setMaxProgress(duration: Int) {
        binding.playerTransport.progressMusic.max = duration
    }

    private fun startMusic() {
        binding.playerTransport.imgPlay.setImageResource(R.drawable.ic_pause_music)
    }

    private fun pauseMusic() {
        binding.playerTransport.imgPlay.setImageResource(R.drawable.ic_play_music)
    }

    private fun resetFavourite() {
        isFavourite = false
        playerPageBinding?.let { pb ->
            pb.imgFavourite.setImageResource(R.drawable.ic_favourite_thin)
            pb.imgFavourite.imageTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.white))
        }
    }

    override fun onDestroyView() {
        swipeDownDismissHelper?.detach()
        swipeDownDismissHelper = null
        lineLyricsAdapter?.onLineClickListener = null
        singerTabListener?.let { listener ->
            pagerAdapter.singerPageBinding?.tabSingers?.removeOnTabSelectedListener(listener)
        }
        singerTabListener = null
        lastSingerTabSongId = null
        binding.playerPager.unregisterOnPageChangeCallback(playerPagerCallback)
        super.onDestroyView()
    }
}
