package com.example.serviceandroid

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.serviceandroid.adapter.AdvertisementAdapter
import com.example.serviceandroid.adapter.PagerNationalAdapter
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TopicAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseActivity
import com.example.serviceandroid.databinding.ActivityMainBinding
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.helper.Data
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.model.Advertisement
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.Topic
import com.example.serviceandroid.service.HelloService
import kotlin.math.abs


@Suppress("DEPRECATION")
class MainActivity : BaseActivity() {
    private lateinit var mSong: Song
    private var isPlaying: Boolean = false
    private lateinit var progressMusic: CountDownTimer
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

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
        setContentView(binding.root)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(Constants.SEND_DATA_TO_ACTIVITY))

        binding.tvZingChat.let {
            val paint = it.paint
            val width = paint.measureText(it.text.toString())
            val textShader: Shader = LinearGradient(
                0f,
                0f,
                width,
                it.textSize,
                intArrayOf(Color.CYAN, Color.MAGENTA, Color.YELLOW),
                null,
                Shader.TileMode.CLAMP
            )
            it.paint.shader = textShader
        }


        binding.startMusic.setOnClickListener {
            clickStartService()
        }

        binding.stopMusic.setOnClickListener {
            sendActionToService(Action.ACTION_CLEAR)
        }

        binding.tvAllNational.isSelected = true
        binding.tvAllNational.setOnClickListener {
            unSelectTvNational()
            binding.tvAllNational.isSelected = true
        }

        binding.tvVietNam.setOnClickListener {
            unSelectTvNational()
            binding.tvVietNam.isSelected = true
        }

        binding.tvInternational.setOnClickListener {
            unSelectTvNational()
            binding.tvInternational.isSelected = true
        }

        initAdvertisement()
        initTopic()
        initNewRelease()
        initNewUpdate()
    }

    private fun initNewUpdate() {
        val adapter = PagerNewReleaseAdapter(this, type = TypeList.TYPE_NEW_UPDATE)
        adapter.songs = Data.listMusic().take(5) as ArrayList<Song>
        binding.rcvNewupdate.adapter = adapter
        adapter.onClickItem = {
            val intent = Intent(this, MusicActivity::class.java)
            intent.putExtra(INDEX_MUSIC, it)
            startActivity(intent)
        }
    }

    private fun initNewRelease() {
        val adapter = PagerNationalAdapter(this, type = TypeList.TYPE_NATIONAL)
        adapter.pagerSong = songsToHashMap(Data.listMusic())
        binding.pagerNewRelease.adapter = adapter
        adapter.onClickItem = {
            val intent = Intent(this, MusicActivity::class.java)
            intent.putExtra(INDEX_MUSIC, it)
            startActivity(intent)
        }
        setUpTransformer(binding.pagerNewRelease, 5, 1f, 0f)
    }

    private fun songsToHashMap(songs: ArrayList<Song>): HashMap<Int, ArrayList<Song>> {
        val hashMap = HashMap<Int, ArrayList<Song>>()
        var index = 0
        var list = arrayListOf<Song>()
        for (i in 0 until songs.size) {
            if (list.size < 3) {
                list.add(songs[i])
                if((i+1) == songs.size) {
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
        val adapter = TopicAdapter(this)
        adapter.topics = topic
        binding.rcvTopic.adapter = adapter
    }

    private fun setUpTransformer(vpg2: ViewPager2, margin: Int, a: Float, b: Float) {
        val transformer = CompositePageTransformer()
        transformer.addTransformer(MarginPageTransformer(margin))
        transformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = a + r * b
        }
        vpg2.setPageTransformer(transformer)
        vpg2.offscreenPageLimit = 3
        vpg2.clipToPadding = false
        vpg2.clipChildren = false
        vpg2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
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
        val adapter = AdvertisementAdapter(this)
        adapter.advertisements = advertisements
        binding.advertisement.adapter = adapter

        setUpTransformer(binding.advertisement, 5, 1f, 0f)
    }

    private fun clickStopService() {
        val intent = Intent(this, HelloService::class.java)
        stopService(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun clickStartService() {
        val intent = Intent(this, HelloService::class.java)
        val song = Song("Lạ Lùng", "Vũ", R.drawable.la_lung, R.raw.la_lung, 262, 0)
        intent.putExtra(MESSAGE_MAIN, song)
        startService(intent)
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
                setStatusButtonPlay()
            }

            Action.ACTION_RESUME -> {
                progressMusic.start()
                setStatusButtonPlay()
            }

            Action.ACTION_PAUSE -> {
                progressMusic.cancel()
                setStatusButtonPlay()
            }

            else -> {
                binding.bottomPlay.visibility = View.GONE
                progressMusic.cancel()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showInfoSong() {
        binding.avatar.setImageResource(mSong.avatar)
        binding.title.text = mSong.title
        binding.nameSingle.text = "Ca sĩ: ${mSong.nameSinger}"
        binding.bottomPlay.visibility = View.VISIBLE
        binding.progressMusic.max = mSong.time
        binding.progressMusic.progress = 0

        progressMusic = object : CountDownTimer(((mSong.time * 1000).toLong()), 1000) {
            override fun onTick(p0: Long) {
                binding.progressMusic.progress += 1
            }

            override fun onFinish() {
                sendActionToService(Action.ACTION_PAUSE)
            }

        }.start()

        binding.play.setOnClickListener {
            if (binding.progressMusic.progress == binding.progressMusic.max && !isPlaying) {
                sendActionToService(Action.ACTION_START)
            } else {
                sendActionToService(if (isPlaying) Action.ACTION_PAUSE else Action.ACTION_RESUME)
            }
        }

        binding.close.setOnClickListener {
            sendActionToService(Action.ACTION_CLEAR)
        }
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
}