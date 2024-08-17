package com.example.serviceandroid.fragment.zingchart

import android.content.Intent
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
import com.bumptech.glide.Glide
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.MusicActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.custom.CustomLineChartRenderer
import com.example.serviceandroid.custom.CustomXAxisFormatter
import com.example.serviceandroid.custom.DialogConfirm
import com.example.serviceandroid.databinding.FragmentZingChartBinding
import com.example.serviceandroid.helper.Data
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
    private val viewModel by viewModels<ZingChartViewModel>()

    override fun initView() {
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.la_lung)
        initGradientText()
        binding.header.title.text = "#zingchart"
        setColorTint(binding.header.search, R.color.white)
        setColorTint(binding.header.micro, R.color.white)

        binding.timeCurrent.text = DateUtils.getTimeWithHourCurrent()

        initSongSuggest()
        initListSongChart()
        initChart()
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

    private fun initSongSuggest() {
        val randomIndex = Random.nextInt(Data.listMusic().size)
        val randomElement = Data.listMusic()[randomIndex]
        Glide.with(requireActivity())
            .load(randomElement.avatar)
            .into(binding.imgSong)
        binding.tvNameSong.text = randomElement.title
        binding.tvNameSinger.text = randomElement.nameSinger
        binding.songSuggestView.setOnClickListener {
            val intent = Intent(requireActivity(), MusicActivity::class.java)
            intent.putExtra(MainActivity.ID_MUSIC, randomElement.idSong)
            startActivity(intent)
        }
    }

    private fun initListSongChart() {
        val adapter = PagerNewReleaseAdapter(requireActivity(), type = TypeList.TYPE_NEW_UPDATE)
        adapter.items = Data.listMusic()
        binding.rcvSongChart.adapter = adapter
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

    private fun initChart() {
        val hours = DateUtils.getLast12Hours()
        val entries1 = ArrayList<Entry>()
        entries1.apply {
            add(Entry(0f, 100f))
            add(Entry(1f, 89f))
            add(Entry(2f, 95f))
            add(Entry(3f, 92f))
            add(Entry(4f, 96f))
            add(Entry(5f, 90f))
            add(Entry(6f, 95f))
            add(Entry(7f, 95f))
            add(Entry(8f, 88f))
            add(Entry(9f, 90f))
            add(Entry(10f, 85f))
            add(Entry(11f, 84f))
        }

        val dataSet1 = LineDataSet(entries1, "Sample Data")
        dataSet1.apply {
            color = requireActivity().getColor(R.color.blue1) // Màu đường biểu đồ
            lineWidth = 1.5f // Độ dày của đường
            circleRadius = 4f // Kích thước của điểm
            circleHoleRadius = 2.8f
            setCircleColor(requireActivity().getColor(R.color.blue1))
            setDrawValues(false)
            setDrawCircles(true)
        }

        val entries2 = ArrayList<Entry>()
        entries2.apply {
            add(Entry(0f, 63f))
            add(Entry(1f, 55f))
            add(Entry(2f, 46f))
            add(Entry(3f, 54f))
            add(Entry(4f, 44f))
            add(Entry(5f, 39f))
            add(Entry(6f, 40f))
            add(Entry(7f, 58f))
            add(Entry(8f, 54f))
            add(Entry(9f, 46f))
            add(Entry(10f, 54f))
            add(Entry(11f, 54f))
        }

        val dataSet2 = LineDataSet(entries2, "Sample Data")
        dataSet2.apply {
            color = requireActivity().getColor(R.color.green3) // Màu đường biểu đồ
            lineWidth = 1.5f // Độ dày của đường
            circleRadius = 4f // Kích thước của điểm
            circleHoleRadius = 2.8f
            setCircleColor(requireActivity().getColor(R.color.green3))
            setDrawValues(false)
            setDrawCircles(false)
        }

        val entries3 = ArrayList<Entry>()
        entries3.apply {
            add(Entry(0f, 10f))
            add(Entry(1f, 20f))
            add(Entry(2f, 15f))
            add(Entry(3f, 16f))
            add(Entry(4f, 5f))
            add(Entry(5f, 10f))
            add(Entry(6f, 27f))
            add(Entry(7f, 30f))
            add(Entry(8f, 30f))
            add(Entry(9f, 22f))
            add(Entry(10f, 19f))
            add(Entry(11f, 28f))
        }

        val dataSet3 = LineDataSet(entries3, "Sample Data")
        dataSet3.apply {
            color = requireActivity().getColor(R.color.brown) // Màu đường biểu đồ
            lineWidth = 1.5f // Độ dày của đường
            circleRadius = 4f // Kích thước của điểm
            circleHoleRadius = 2.8f
            setCircleColor(requireActivity().getColor(R.color.brown))
            setDrawValues(false)
            setDrawCircles(false)
        }

        val lineData = LineData(dataSet1, dataSet2, dataSet3)
        binding.chart.data = lineData
        // Sử dụng CustomXAxisFormatter để hiển thị đúng các nhãn trục x
        binding.chart.xAxis.valueFormatter = CustomXAxisFormatter(hours)

        binding.chart.description = Description().apply { text = "" }
        binding.chart.legend.isEnabled = false
        binding.chart.extraBottomOffset = 10f
        binding.chart.extraTopOffset = 30f
        binding.chart.setScaleEnabled(false)
        lineData.isHighlightEnabled = false

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

        binding.chart.animateX(1000) // Hiệu ứng animate

        // Tạo Custom Renderer và áp dụng nó
        binding.chart.renderer = bitmap?.let { CustomLineChartRenderer(requireActivity(), binding.chart, 0, 4, R.color.blue1, it) }

        // Cập nhật biểu đồ
        binding.chart.invalidate() // Vẽ lại biểu đồ với dữ liệu mới

        binding.chart.setOnClickListener {
            resetHandlerUpdateIndexLineChart()
            updateIndexLineChart(dataSet1, dataSet2, dataSet3)
            binding.chart.invalidate()
        }

        runnable = object : Runnable {
            override fun run() {
                Log.e("Time Test:", DateUtils.getTimeCurrent())
                updateIndexLineChart(dataSet1, dataSet2, dataSet3)
                binding.chart.invalidate() // Vẽ lại biểu đồ với dữ liệu mới
                handler.postDelayed(this,5000)
            }

        }

        runnable?.let {
            handler.postDelayed(it, 5000)
        }
    }

    private fun updateIndexLineChart(dataSet1: LineDataSet, dataSet2: LineDataSet, dataSet3: LineDataSet) {
        when(positionChart) {
            PositionChart.LineChart1 -> {
                positionChart = PositionChart.LineChart2
                bitmap = BitmapFactory.decodeResource(resources, Data.listMusic()[1].avatar)
                binding.chart.renderer = bitmap?.let {
                    CustomLineChartRenderer(
                        requireActivity(),
                        binding.chart,
                        positionChart.ordinal,
                        5,
                        R.color.green3,
                        it
                    )
                }
                dataSet1.setDrawCircles(false)
                dataSet2.setDrawCircles(true)
            }

            PositionChart.LineChart2 -> {
                positionChart = PositionChart.LineChart3
                bitmap = BitmapFactory.decodeResource(resources, Data.listMusic()[2].avatar)
                binding.chart.renderer = bitmap?.let {
                    CustomLineChartRenderer(
                        requireActivity(),
                        binding.chart,
                        positionChart.ordinal,
                        8,
                        R.color.brown,
                        it
                    )
                }
                dataSet2.setDrawCircles(false)
                dataSet3.setDrawCircles(true)
            }

            else -> {
                positionChart = PositionChart.LineChart1
                bitmap = BitmapFactory.decodeResource(resources, Data.listMusic()[0].avatar)
                binding.chart.renderer = bitmap?.let {
                    CustomLineChartRenderer(
                        requireActivity(),
                        binding.chart,
                        positionChart.ordinal,
                        4,
                        R.color.blue1,
                        it
                    )
                }
                dataSet3.setDrawCircles(false)
                dataSet1.setDrawCircles(true)
            }
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

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentZingChartBinding.inflate(inflater)

}