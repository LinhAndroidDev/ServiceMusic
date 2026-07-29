package com.example.serviceandroid.fragment.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.AdvertisementAdapter
import com.example.serviceandroid.adapter.PagerNationalAdapter
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TopicAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.FragmentHomeBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.model.Advertisement
import com.example.serviceandroid.model.National
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.Topic
import com.example.serviceandroid.utils.Constant
import com.example.serviceandroid.utils.ExtensionFunctions
import com.example.serviceandroid.utils.ExtensionFunctions.isViewVisible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class Title {
    TITLE_TOPIC,
    TITLE_NEW_RELEASE,
}

private const val BANNER_AUTO_SCROLL_MS = 5_000L

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private var national = National.ALL_NATIONAL
    private var adapterNational: PagerNationalAdapter? = null
    private var newUpdateAdapter: PagerNewReleaseAdapter? = null
    private var newReleasePagerConfigured = false
    private var advertisementAdapter: AdvertisementAdapter? = null
    private var advertisementBannerConfigured = false
    private var bannerItemWidthPx = 0
    private var currentBannerIndex = 0
    private var lastBoundAdvertisementIds: List<String> = emptyList()
    private val bannerHandler = Handler(Looper.getMainLooper())
    private var bannerAutoScrollRunnable: Runnable? = null
    private var bannerScrollListener: RecyclerView.OnScrollListener? = null
    private var stickTile = Title.TITLE_TOPIC
    private val viewModel by viewModels<HomeViewModel>()

    override fun initView() {
        binding.titleCover.isVisible = binding.scrollHome.isViewVisible(binding.titleTopic)
        stickHeader()

        /**
         * Change Color According To Text Gradient
         */
        ExtensionFunctions.gradientTextColor(binding.tvZingChat)

        initTopic()
        setupSwipeRefresh()
        observeAdvertisements()
        observePlaylist()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            R.color.blue,
            R.color.bg_purple,
            R.color.orange,
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAll()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var wasRefreshing = false
                viewModel.isRefreshing.collect { refreshing ->
                    binding.swipeRefresh.isRefreshing = refreshing
                    if (wasRefreshing && !refreshing) {
                        rebindAfterRefresh()
                    }
                    wasRefreshing = refreshing
                }
            }
        }
    }

    private fun rebindAfterRefresh() {
        val songs = viewModel.getPlaylist()
        if (songs.isNotEmpty()) {
            applyPlaylistToUi(songs)
        }
        bindAdvertisements(viewModel.getAdvertisements(), force = true)
    }

    private fun observeAdvertisements() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadAdvertisements()
                viewModel.advertisements.collect { ads ->
                    bindAdvertisements(ads)
                }
            }
        }
    }

    private fun bindAdvertisements(ads: List<Advertisement>, force: Boolean = false) {
        if (ads.isEmpty()) {
            stopBannerAutoScroll()
            binding.advertisementBanner.adapter = null
            binding.bannerIndicator.isVisible = false
            lastBoundAdvertisementIds = emptyList()
            return
        }

        val itemWidth = calculateBannerItemWidth()
        setupAdvertisementBanner(itemWidth)

        val adapter = advertisementAdapter?.takeIf { it.itemWidthPx == itemWidth }
            ?: AdvertisementAdapter(itemWidth).also {
                advertisementAdapter = it
                binding.advertisementBanner.adapter = it
            }
        if (binding.advertisementBanner.adapter !== adapter) {
            binding.advertisementBanner.adapter = adapter
        }

        val newIds = ads.map { it.id.ifBlank { "${it.image}|${it.update}" } }
        if (force || newIds != lastBoundAdvertisementIds) {
            lastBoundAdvertisementIds = newIds
            adapter.submitAdvertisements(ads)
            currentBannerIndex = 0
            binding.advertisementBanner.scrollToPosition(adapter.getInfiniteStartPosition())
            binding.advertisementBanner.post {
                applyBannerDepthEffect(binding.advertisementBanner)
            }
        }
        updateBannerIndicator(currentBannerIndex)
        startBannerAutoScroll()
    }

    private fun updateBannerIndicator(selectedRealIndex: Int) {
        val count = advertisementAdapter?.realItemCount ?: 0
        binding.bannerIndicator.isVisible = count > 1
        if (count <= 1) return
        binding.bannerIndicator.setDotCount(count)
        binding.bannerIndicator.setCurrentPosition(selectedRealIndex.coerceIn(0, count - 1))
    }

    private fun calculateBannerItemWidth(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        return (screenWidth * 0.78f).toInt()
    }

    private fun applyBannerSidePadding(itemWidth: Int) {
        val screenWidth = resources.displayMetrics.widthPixels
        val sidePadding = (screenWidth - itemWidth) / 2
        binding.advertisementBanner.setPadding(sidePadding, 0, sidePadding, 0)
    }

    private fun setupAdvertisementBanner(itemWidth: Int) {
        val recyclerView = binding.advertisementBanner
        applyBannerSidePadding(itemWidth)
        bannerItemWidthPx = itemWidth

        if (advertisementBannerConfigured) return

        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView.clipToPadding = false
        recyclerView.clipChildren = false
        recyclerView.setHasFixedSize(true)

        bannerScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                applyBannerDepthEffect(recyclerView)
                updateBannerIndexFromSnap(recyclerView)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> stopBannerAutoScroll()
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        applyBannerDepthEffect(recyclerView)
                        updateBannerIndexFromSnap(recyclerView)
                        repositionInfiniteBannerIfNeeded(recyclerView)
                        startBannerAutoScroll()
                    }
                }
            }
        }.also { recyclerView.addOnScrollListener(it) }

        advertisementBannerConfigured = true
    }

    private fun applyBannerDepthEffect(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager ?: return
        val center = recyclerView.width / 2f
        var centerChild: android.view.View? = null
        var minDistance = Float.MAX_VALUE

        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val childCenter = (child.left + child.right) / 2f
            val distance = abs(childCenter - center) / center
            val scale = 1f - (distance * 0.2f).coerceIn(0f, 0.2f)
            val alpha = 1f - (distance * 0.45f).coerceIn(0f, 0.5f)
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = alpha

            val centerDistance = abs(childCenter - center)
            if (centerDistance < minDistance) {
                minDistance = centerDistance
                centerChild = child
            }
        }

        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            child.translationZ = if (child === centerChild) 12f else 0f
        }
    }

    private fun updateBannerIndexFromSnap(recyclerView: RecyclerView) {
        val adapter = advertisementAdapter ?: return
        val snapped = findBannerSnapPosition(recyclerView)
        if (snapped == RecyclerView.NO_POSITION) return
        val realIndex = adapter.toRealIndex(snapped)
        if (realIndex != currentBannerIndex) {
            currentBannerIndex = realIndex
            updateBannerIndicator(realIndex)
        }
    }

    private fun repositionInfiniteBannerIfNeeded(recyclerView: RecyclerView) {
        val adapter = advertisementAdapter ?: return
        val realCount = adapter.realItemCount
        if (realCount <= 1) return

        val snapped = findBannerSnapPosition(recyclerView)
        if (snapped == RecyclerView.NO_POSITION) return

        val totalCount = adapter.itemCount
        val threshold = realCount * 2
        val middleOffset = adapter.getInfiniteMiddleOffset()
        val newPosition = when {
            snapped < threshold -> snapped + middleOffset
            snapped > totalCount - threshold -> snapped - middleOffset
            else -> return
        }
        recyclerView.scrollToPosition(newPosition)
    }

    private fun findBannerSnapPosition(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager ?: return RecyclerView.NO_POSITION
        val snapTarget = recyclerView.width / 2
        var minDistance = Int.MAX_VALUE
        var position = RecyclerView.NO_POSITION
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val childCenter = (child.left + child.right) / 2
            val distance = abs(childCenter - snapTarget)
            if (distance < minDistance) {
                minDistance = distance
                position = recyclerView.getChildAdapterPosition(child)
            }
        }
        return position
    }

    private fun startBannerAutoScroll() {
        stopBannerAutoScroll()
        val realCount = advertisementAdapter?.realItemCount ?: 0
        if (realCount <= 1 || !isResumed) return

        bannerAutoScrollRunnable = object : Runnable {
            override fun run() {
                if (!isResumed || !isAdded) return
                val adapter = advertisementAdapter ?: return
                if (adapter.realItemCount <= 1) return

                val recyclerView = binding.advertisementBanner
                val currentAdapterPosition = findBannerSnapPosition(recyclerView)
                if (currentAdapterPosition == RecyclerView.NO_POSITION) return

                recyclerView.smoothScrollToPosition(currentAdapterPosition + 1)
                bannerHandler.postDelayed(this, BANNER_AUTO_SCROLL_MS)
            }
        }
        bannerHandler.postDelayed(bannerAutoScrollRunnable!!, BANNER_AUTO_SCROLL_MS)
    }

    private fun stopBannerAutoScroll() {
        bannerAutoScrollRunnable?.let { bannerHandler.removeCallbacks(it) }
        bannerAutoScrollRunnable = null
    }

    private fun observePlaylist() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ensureLoaded()
                viewModel.playlist.collect { songs ->
                    if (songs.isEmpty()) return@collect
                    applyPlaylistToUi(songs)
                }
            }
        }
    }

    /**
     * Re-binds adapters every time playlist updates. Required after [onDestroyView] recreates
     * the layout but the fragment instance (and adapter fields) may still exist.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun applyPlaylistToUi(songs: List<Song>) {
        val nationalAdapter = adapterNational ?: PagerNationalAdapter(
            requireActivity(),
            type = TypeList.TYPE_NATIONAL,
        ).also { adapter ->
            adapter.onClickItem = { songId ->
                MusicPlayerLauncher.open(this, songId)
            }
            adapter.onClickMoreOption = { song -> showMoreOptions(song) }
            adapterNational = adapter
        }

        val filtered = songs.filter { it.checkMusicNational(national) }
        nationalAdapter.resetList(songsToHashMap(ArrayList(filtered)))
        binding.pagerNewRelease.adapter = nationalAdapter
        if (!newReleasePagerConfigured) {
            setUpViewPagerTransformer(binding.pagerNewRelease, 5, 1f, 0f)
            newReleasePagerConfigured = true
        }

        val chartAdapter = newUpdateAdapter ?: PagerNewReleaseAdapter(
            requireActivity(),
            type = TypeList.TYPE_NEW_UPDATE,
        ).also { adapter ->
            adapter.onClickItem = { songId ->
                MusicPlayerLauncher.open(this, songId)
            }
            adapter.onClickMoreOption = { song -> showMoreOptions(song) }
            newUpdateAdapter = adapter
        }
        chartAdapter.items = ArrayList(songs.take(5))
        binding.rcvNewupdate.adapter = chartAdapter
        chartAdapter.notifyDataSetChanged()
    }

    private fun showMoreOptions(song: Song) {
        val dialog = BottomSheetOptionMusic()
        dialog.removeFavourite = {
            showDialogConfirmRemoveFavourite(song)
        }
        val bundle = Bundle()
        bundle.putParcelable(Constant.KEY_SONG, song)
        dialog.arguments = bundle
        dialog.show(parentFragmentManager, "")
    }

    /**
     * Create Stick Header When Scroll
     */
    @SuppressLint("SetTextI18n")
    private fun stickHeader() {
        binding.scrollHome.setOnScrollChangeListener { _, _, _, _, _ ->
            when (stickTile) {
                Title.TITLE_TOPIC -> {
                    binding.tvCover.text = "Chủ đề & thể loại"
                    binding.titleCover.isVisible =
                        if (binding.scrollHome.isViewVisible(binding.titleTopic)) {
                            stickTile = Title.TITLE_NEW_RELEASE
                            true
                        } else {
                            stickTile = Title.TITLE_TOPIC
                            false
                        }
                }

                Title.TITLE_NEW_RELEASE -> {
                    binding.tvCover.text =
                        if (binding.scrollHome.isViewVisible(binding.titleNewRelease)) {
                            "Mới phát hành"
                        } else {
                            stickTile = Title.TITLE_TOPIC
                            "Chủ đề & thể loại"
                        }

                }
            }
        }
    }

    /**
     * Catch Click View Components Event
     */
    override fun onClickView() {
        binding.tvAllNational.isSelected = true
        val evenClick = mapOf(
            binding.tvAllNational to Pair(National.ALL_NATIONAL, binding.tvAllNational),
            binding.tvVietNam to Pair(National.VIETNAMESE, binding.tvVietNam),
            binding.tvInternational to Pair(National.INTERNATIONAL, binding.tvInternational)
        )

        evenClick.forEach { (view, pair) ->
            view.setOnClickListener {
                lifecycleScope.launch {
                    async {
                        unSelectTvNational()
                        pair.second.isSelected = true
                        national = pair.first
                    }.await()
                    async {
                        resetMusicInterNational()
                    }.await()
                }
            }
        }

        binding.header.search.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_fragmentSearchSong)
        }

        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_zingchartFragment)
        }
    }

    private fun resetMusicInterNational() {
        val songs = viewModel.getPlaylist().filter { it.checkMusicNational(national) }
        adapterNational?.resetList(songsToHashMap(ArrayList(songs)))
    }

    private fun showDialogConfirmRemoveFavourite(song: Song) {
        DialogConfirm().apply {
            title = song.title
            onClickRemove = {
                viewModel.deleteSongById(song.id) {
                    Toast.makeText(
                        requireActivity(),
                        "Đã xoá khỏi bài hát yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.show(requireActivity().supportFragmentManager, "")
    }

    private fun songsToHashMap(songs: ArrayList<Song>): HashMap<Int, ArrayList<Song>> {
        val hashMap = HashMap<Int, ArrayList<Song>>()
        var index = 0
        var list = arrayListOf<Song>()
        for (i in 0 until songs.size) {
            if (list.size < 3) {
                list.add(songs[i])
                if ((i + 1) == songs.size) {
                    hashMap[index] = list
                }
            } else {
                hashMap[index] = list
                list = ArrayList()
                index++
                list.add(songs[i])
            }
        }
        return hashMap
    }

    private fun unSelectTvNational() {
        binding.tvAllNational.isSelected = false
        binding.tvVietNam.isSelected = false
        binding.tvInternational.isSelected = false
    }

    private fun initTopic() {
        val topic = arrayListOf<Topic>()
        topic.add(Topic(R.drawable.ic_music, "BXH Nhạc Mới", R.color.bg_blue))
        topic.add(Topic(R.drawable.ic_star, "Top 100", R.color.bg_purple))
        topic.add(Topic(null, "Nhạc Việt", R.color.bg_orange))
        topic.add(Topic(null, "Nhạc Hoa", R.color.bg_pink))
        topic.add(Topic(null, "Nhạc Âu Mỹ", R.color.bg_green1))
        topic.add(Topic(null, "Nhạc Hàn", R.color.bg_green2))
        topic.add(Topic(null, null, null))
        val adapter = TopicAdapter(requireActivity())
        adapter.items = topic
        binding.rcvTopic.adapter = adapter
    }

    private fun setUpViewPagerTransformer(vpg2: ViewPager2, margin: Int, a: Float, b: Float) {
        val transformer = CompositePageTransformer()
        transformer.addTransformer(MarginPageTransformer(margin))
        transformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = a + r * b
        }
        vpg2.apply {
            setPageTransformer(transformer)
            offscreenPageLimit = 3
            clipToPadding = false
            clipChildren = false
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    override fun onResume() {
        super.onResume()
        startBannerAutoScroll()
    }

    override fun onPause() {
        stopBannerAutoScroll()
        super.onPause()
    }

    override fun onDestroyView() {
        stopBannerAutoScroll()
        bannerScrollListener?.let { binding.advertisementBanner.removeOnScrollListener(it) }
        bannerScrollListener = null
        binding.pagerNewRelease.adapter = null
        binding.rcvNewupdate.adapter = null
        binding.advertisementBanner.adapter = null
        newReleasePagerConfigured = false
        advertisementBannerConfigured = false
        currentBannerIndex = 0
        lastBoundAdvertisementIds = emptyList()
        super.onDestroyView()
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentHomeBinding.inflate(inflater)
}