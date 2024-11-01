package com.example.serviceandroid.fragment.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.MusicActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.AdvertisementAdapter
import com.example.serviceandroid.adapter.PagerNationalAdapter
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TopicAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.custom.OverlapItemDecoration
import com.example.serviceandroid.databinding.FragmentHomeBinding
import com.example.serviceandroid.helper.Data
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

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private var national = National.ALL_NATIONAL
    private lateinit var adapterNational: PagerNationalAdapter
    private var stickTile = Title.TITLE_TOPIC
    private val viewModel by viewModels<HomeViewModel>()

    override fun initView() {
        binding.titleCover.isVisible = binding.scrollHome.isViewVisible(binding.titleTopic)
        stickHeader()

        /**
         * Change Color According To Text Gradient
         */
        ExtensionFunctions.gradientTextColor(binding.tvZingChat)

        binding.root.postDelayed(500) {
            initializeViews()
        }
    }

    private fun initializeViews() {
        initAdvertisement()
        initTopic()
        binding.root.postDelayed(1000) {
            initNewRelease()
            binding.root.postDelayed(1500) {
                initNewUpdate()
            }
        }
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
        adapterNational.resetList(
            songsToHashMap(Data.listMusic().filter {
                it.checkMusicNational(national)
            } as ArrayList<Song>)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initNewUpdate() {
        val adapter = PagerNewReleaseAdapter(requireActivity(), type = TypeList.TYPE_NEW_UPDATE)
        adapter.items = Data.listMusic().take(5) as ArrayList<Song>
        binding.rcvNewupdate.adapter = adapter
        adapter.onClickItem = {
            val intent = Intent(requireActivity(), MusicActivity::class.java)
            intent.putExtra(MainActivity.ID_MUSIC, it)
            startActivity(intent)
        }
        adapter.onClickMoreOption = { song ->
            val dialog = BottomSheetOptionMusic()
            dialog.removeFavourite = {
                showDialogConfirmRemoveFavourite(song)
            }
            val bundle = Bundle()
            bundle.putParcelable(Constant.KEY_SONG, song)
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "")

        }
    }

    private fun initNewRelease() {
        adapterNational = PagerNationalAdapter(requireActivity(), type = TypeList.TYPE_NATIONAL)
        adapterNational.pagerSong = songsToHashMap(Data.listMusic())
        binding.pagerNewRelease.adapter = adapterNational
        adapterNational.onClickItem = {
            val intent = Intent(requireActivity(), MusicActivity::class.java)
            intent.putExtra(MainActivity.ID_MUSIC, it)
            startActivity(intent)
        }
        adapterNational.onClickMoreOption = { song ->
            val dialog = BottomSheetOptionMusic()
            dialog.removeFavourite = {
                showDialogConfirmRemoveFavourite(song)
            }
            val bundle = Bundle()
            bundle.putParcelable(Constant.KEY_SONG, song)
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "")
        }
        setUpViewPagerTransformer(binding.pagerNewRelease, 5, 1f, 0f)
    }

    private fun showDialogConfirmRemoveFavourite(song: Song) {
        DialogConfirm().apply {
            title = song.title
            onClickRemove = {
                viewModel.deleteSongById(song.idSong) {
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

    private fun initAdvertisement() {
        val advertisements = arrayListOf<Advertisement>()
        advertisements.add(
            Advertisement(
                "https://photo-resize-zmp3.zmdcdn.me/w600_r1x1_jpeg/banner/2/7/b/d/27bdc67fef29c7928298c5759de08534.jpg",
                "Hay nhất của V-POP",
                "Thiên  Lý Ơi đưa  Jack - J97 trở lại với Top Trending"
            )
        )
        advertisements.add(
            Advertisement(
                "https://source.boomplaymusic.com/group10/M00/02/06/f9d04bde573f4737a9859f386331d68b_320_320.jpg",
                "Mới Cập Nhật",
                "Có Lẽ Bên Nhau Là Sai và những bản Hit tiềm năng"
            )
        )
        advertisements.add(
            Advertisement(
                "https://i.ytimg.com/vi/yF1rUhDRzG0/maxresdefault.jpg",
                "Mới Cập Nhật",
                "Bản Hit Đánh Mất Em mới lạ qua giọng hát của các ca sĩ trẻ"
            )
        )
        val adapter = AdvertisementAdapter(requireActivity())
        adapter.advertisements = advertisements
        binding.advertisement.adapter = adapter
        LinearSnapHelper().attachToRecyclerView(binding.advertisement)

        // Set ItemDecoration to add overlap/margin between items
        binding.advertisement.addItemDecoration(
            OverlapItemDecoration(
                resources.getDimensionPixelSize(R.dimen.item_overlap_width),
                resources.getDimensionPixelSize(R.dimen.item_overlap_width)
            )
        )
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentHomeBinding.inflate(inflater)
}