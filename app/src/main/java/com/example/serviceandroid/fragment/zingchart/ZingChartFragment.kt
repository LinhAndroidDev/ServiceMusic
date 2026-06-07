package com.example.serviceandroid.fragment.zingchart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.CustomLineChartRenderer
import com.example.serviceandroid.custom.CustomXAxisFormatter
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.FragmentZingChartBinding
import com.example.serviceandroid.model.PositionChart
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.Constant
import com.example.serviceandroid.utils.DateUtils
import com.example.serviceandroid.utils.ExtensionFunctions.setColorTint
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

@AndroidEntryPoint
class ZingChartFragment : BaseFragment<FragmentZingChartBinding>() {
    private var positionChart: PositionChart = PositionChart.LineChart1
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var runnable: Runnable? = null
    private var bitmap: Bitmap? = null
    private var songChartAdapter: PagerNewReleaseAdapter? = null
    private var suggestedSongId: String? = null
    private var chartInitialized = false
    private var chartData: ChartData? = null
    private val viewModel by viewModels<ZingChartViewModel>()

    private data class ChartData(
        val lineData: LineData,
        val dataSet1: LineDataSet,
        val dataSet2: LineDataSet,
        val dataSet3: LineDataSet,
    )

    override fun initView() {
        initGradientText()
        binding.header.title.text = "#zingchart"
        setColorTint(binding.header.search, R.color.white)
        setColorTint(binding.header.micro, R.color.white)
        binding.timeCurrent.text = DateUtils.getTimeWithHourCurrent()

        setupSwipeRefresh()
        observePlaylist()

        // Defer heavy chart work so the tab transition can finish on the next frame.
        binding.root.post { prepareChartDeferred() }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            R.color.blue1,
            R.color.green3,
            R.color.orange,
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTopSongs()
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
        binding.timeCurrent.text = DateUtils.getTimeWithHourCurrent()
        val playlist = viewModel.getPlaylist()
        if (playlist.isNotEmpty()) {
            applyPlaylistToUi(playlist, force = true)
        }
    }

    private fun prepareChartDeferred() {
        if (!isAdded) return
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(resources, R.drawable.la_lung)
        }
        setupChart(animate = !chartInitialized)
        chartInitialized = true
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

    private fun applyPlaylistToUi(playlist: List<Song>, force: Boolean = false) {
        bindSongChartList(playlist, force = force)
        bindSongSuggest(playlist)
    }

    override fun onClickView() {
        binding.removeSongSuggest.setOnClickListener {
            binding.songSuggestView.isVisible = false
        }
    }

    private fun initGradientText() {
        binding.header.title.let {
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
    }

    private fun bindSongSuggest(playlist: List<Song>) {
        if (playlist.isEmpty()) return
        val song = suggestedSongId
            ?.let { id -> playlist.find { it.id == id } }
            ?: playlist[Random.nextInt(playlist.size)].also { suggestedSongId = it.id }
        Glide.with(requireActivity())
            .load(song.thumbnailUrl)
            .placeholder(R.drawable.la_lung)
            .error(R.drawable.la_lung)
            .into(binding.imgSong)
        binding.tvNameSong.text = song.title
        binding.tvNameSinger.text = song.nameSinger
        binding.songSuggestView.setOnClickListener {
            val action = ZingChartFragmentDirections.actionZingchartFragmentToFragmentMusic(songId = song.id)
            findNavController().navigate(action)
        }
    }

    private var lastBoundChartIds: List<String> = emptyList()

    private fun bindSongChartList(playlist: List<Song>, force: Boolean = false) {
        val adapter = songChartAdapter ?: PagerNewReleaseAdapter(
            requireActivity(),
            type = TypeList.TYPE_NEW_UPDATE,
        ).also { created ->
            created.onClickItem = { songId ->
                val action = ZingChartFragmentDirections.actionZingchartFragmentToFragmentMusic(songId = songId)
                findNavController().navigate(action)
            }
            created.onClickMoreOption = { song -> showMoreOptions(song) }
            songChartAdapter = created
        }
        val newIds = playlist.map { it.id }
        if (force || newIds != lastBoundChartIds || adapter.items != playlist) {
            lastBoundChartIds = newIds
            adapter.items = ArrayList(playlist)
            adapter.notifyDataSetChanged()
        }
        if (binding.rcvSongChart.adapter !== adapter) {
            binding.rcvSongChart.adapter = adapter
        }
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

    private fun getOrBuildChartData(): ChartData {
        chartData?.let { return it }
        val entries1 = arrayListOf(
            Entry(0f, 100f), Entry(1f, 89f), Entry(2f, 95f), Entry(3f, 92f),
            Entry(4f, 96f), Entry(5f, 90f), Entry(6f, 95f), Entry(7f, 95f),
            Entry(8f, 88f), Entry(9f, 90f), Entry(10f, 85f), Entry(11f, 84f),
        )
        val dataSet1 = LineDataSet(entries1, "Sample Data").apply {
            color = requireActivity().getColor(R.color.blue1)
            lineWidth = 1.5f
            circleRadius = 4f
            circleHoleRadius = 2.8f
            setCircleColor(requireActivity().getColor(R.color.blue1))
            setDrawValues(false)
            setDrawCircles(true)
        }

        val entries2 = arrayListOf(
            Entry(0f, 63f), Entry(1f, 55f), Entry(2f, 46f), Entry(3f, 54f),
            Entry(4f, 44f), Entry(5f, 39f), Entry(6f, 40f), Entry(7f, 58f),
            Entry(8f, 54f), Entry(9f, 46f), Entry(10f, 54f), Entry(11f, 54f),
        )
        val dataSet2 = LineDataSet(entries2, "Sample Data").apply {
            color = requireActivity().getColor(R.color.green3)
            lineWidth = 1.5f
            circleRadius = 4f
            circleHoleRadius = 2.8f
            setCircleColor(requireActivity().getColor(R.color.green3))
            setDrawValues(false)
            setDrawCircles(false)
        }

        val entries3 = arrayListOf(
            Entry(0f, 10f), Entry(1f, 20f), Entry(2f, 15f), Entry(3f, 16f),
            Entry(4f, 5f), Entry(5f, 10f), Entry(6f, 27f), Entry(7f, 30f),
            Entry(8f, 30f), Entry(9f, 22f), Entry(10f, 19f), Entry(11f, 28f),
        )
        val dataSet3 = LineDataSet(entries3, "Sample Data").apply {
            color = requireActivity().getColor(R.color.brown)
            lineWidth = 1.5f
            circleRadius = 4f
            circleHoleRadius = 2.8f
            setCircleColor(requireActivity().getColor(R.color.brown))
            setDrawValues(false)
            setDrawCircles(false)
        }

        return ChartData(LineData(dataSet1, dataSet2, dataSet3), dataSet1, dataSet2, dataSet3)
            .also { chartData = it }
    }

    private fun setupChart(animate: Boolean) {
        val data = getOrBuildChartData()
        val hours = DateUtils.getLast12Hours()
        binding.chart.data = data.lineData
        // Sử dụng CustomXAxisFormatter để hiển thị đúng các nhãn trục x
        binding.chart.xAxis.valueFormatter = CustomXAxisFormatter(hours)

        binding.chart.description = Description().apply { text = "" }
        binding.chart.legend.isEnabled = false
        binding.chart.extraBottomOffset = 10f
        binding.chart.extraTopOffset = 30f
        binding.chart.setScaleEnabled(false)
        data.lineData.isHighlightEnabled = false

        // Cấu hình trục X
        val xAxis: XAxis = binding.chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textColor = requireActivity().getColor(R.color.text_white)
        xAxis.textSize = 12f
        xAxis.labelCount = 12
        xAxis.yOffset = 10f // Tăng khoảng cách giữa nhãn và trục X
        xAxis.axisLineColor = requireActivity().getColor(R.color.grey_blur)
        xAxis.axisLineWidth = 1f
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawLabels(true)
        xAxis.setDrawGridLines(false)

        // Cấu hình trục Y
        binding.chart.axisRight.isEnabled = false // Vô hiệu hóa trục Y bên phải
        binding.chart.axisLeft.granularity = 10f // Đơn vị trên trục Y
        binding.chart.axisRight.setDrawAxisLine(false)
        binding.chart.axisRight.setDrawLabels(false)
        binding.chart.axisRight.setDrawGridLines(false)
        binding.chart.axisLeft.setDrawAxisLine(false)
        binding.chart.axisLeft.setDrawLabels(false)
        binding.chart.axisLeft.setDrawGridLines(false)

        if (animate) {
            binding.chart.animateX(1000)
        }

        binding.chart.renderer = bitmap?.let {
            CustomLineChartRenderer(requireActivity(), binding.chart, 0, 4, R.color.blue1, it)
        }
        binding.chart.invalidate()

        binding.chart.setOnClickListener {
            resetHandlerUpdateIndexLineChart()
            updateIndexLineChart(data.dataSet1, data.dataSet2, data.dataSet3)
            binding.chart.invalidate()
        }

        runnable?.let { handler.removeCallbacks(it) }
        runnable = object : Runnable {
            override fun run() {
                Log.e("Time Test:", DateUtils.getTimeCurrent())
                updateIndexLineChart(data.dataSet1, data.dataSet2, data.dataSet3)
                binding.chart.invalidate()
                handler.postDelayed(this, 5000)
            }
        }
        handler.postDelayed(runnable!!, 5000)
    }

    private fun updateIndexLineChart(dataSet1: LineDataSet, dataSet2: LineDataSet, dataSet3: LineDataSet) {
        when (positionChart) {
            PositionChart.LineChart1 -> {
                positionChart = PositionChart.LineChart2
                val pl = viewModel.getPlaylist()
                applyChartRendererAsync(
                    url = pl.getOrNull(1)?.thumbnailUrl,
                    entryIndex = positionChart.ordinal,
                    indexPoint = 5,
                    colorRes = R.color.green3,
                )
                dataSet1.setDrawCircles(false)
                dataSet2.setDrawCircles(true)
            }

            PositionChart.LineChart2 -> {
                positionChart = PositionChart.LineChart3
                val pl2 = viewModel.getPlaylist()
                applyChartRendererAsync(
                    url = pl2.getOrNull(2)?.thumbnailUrl,
                    entryIndex = positionChart.ordinal,
                    indexPoint = 8,
                    colorRes = R.color.brown,
                )
                dataSet2.setDrawCircles(false)
                dataSet3.setDrawCircles(true)
            }

            else -> {
                positionChart = PositionChart.LineChart1
                val pl0 = viewModel.getPlaylist()
                applyChartRendererAsync(
                    url = pl0.firstOrNull()?.thumbnailUrl,
                    entryIndex = positionChart.ordinal,
                    indexPoint = 4,
                    colorRes = R.color.blue1,
                )
                dataSet3.setDrawCircles(false)
                dataSet1.setDrawCircles(true)
            }
        }
    }

    private fun applyChartRendererAsync(
        url: String?,
        entryIndex: Int,
        indexPoint: Int,
        colorRes: Int,
    ) {
        val placeholder = bitmap ?: BitmapFactory.decodeResource(resources, R.drawable.la_lung)
        binding.chart.renderer = CustomLineChartRenderer(
            requireActivity(),
            binding.chart,
            entryIndex,
            indexPoint,
            colorRes,
            placeholder,
        )
        if (url.isNullOrBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { loadChartAvatarBlocking(url) }
            if (!isAdded || positionChart.ordinal != entryIndex) return@launch
            bitmap = loaded
            binding.chart.renderer = CustomLineChartRenderer(
                requireActivity(),
                binding.chart,
                entryIndex,
                indexPoint,
                colorRes,
                loaded,
            )
            binding.chart.invalidate()
        }
    }

    private fun resetHandlerUpdateIndexLineChart() {
        runnable?.let {
            handler.removeCallbacks(it)
            handler.postDelayed(it, 5000)
        }
    }

    override fun onResume() {
        super.onResume()
        resetHandlerUpdateIndexLineChart()
    }

    override fun onStop() {
        super.onStop()
        runnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroyView() {
        runnable?.let { handler.removeCallbacks(it) }
        binding.rcvSongChart.adapter = null
        lastBoundChartIds = emptyList()
        super.onDestroyView()
    }

    override fun onDestroy() {
        runnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    private fun loadChartAvatarBlocking(url: String): Bitmap {
        return try {
            Glide.with(requireActivity())
                .asBitmap()
                .load(url)
                .submit()
                .get()
        } catch (_: Exception) {
            BitmapFactory.decodeResource(resources, R.drawable.la_lung)
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentZingChartBinding.inflate(inflater)

}