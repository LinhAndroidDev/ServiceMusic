package com.example.serviceandroid.fragment.radio

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.MoodAndActivityAdapter
import com.example.serviceandroid.adapter.RadioAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.ArcLayoutManager
import com.example.serviceandroid.custom.OverlapItemDecoration
import com.example.serviceandroid.databinding.FragmentRadioBinding
import com.example.serviceandroid.model.Radio
import com.example.serviceandroid.utils.ExtensionFunctions.setColorTint

class RadioFragment : BaseFragment<FragmentRadioBinding>() {

    @SuppressLint("ClickableViewAccessibility", "ResourceAsColor")
    override fun initView() {
        binding.header.title.text = "Radio"
        binding.header.title.setTextColor(requireActivity().getColor(R.color.white))
        setColorTint(binding.header.search, R.color.white)
        setColorTint(binding.header.micro, R.color.white)
        val list = arrayListOf<Radio>()
        for (i in 0 until 10) {
            list.add(Radio())
        }
        val radioAdapter = RadioAdapter()
        radioAdapter.onItemClick = { position ->
            val layoutManager = binding.rcvRadio.layoutManager as LinearLayoutManager
            val view = layoutManager.findViewByPosition(position)

            view?.let {
                // Lấy vị trí của view và tính toán offset để đưa item ra giữa
                val viewCenterX = it.left + it.width / 2
                val screenCenterX = binding.rcvRadio.width / 2
                val scrollBy = viewCenterX - screenCenterX

                binding.rcvRadio.smoothScrollBy(scrollBy, 0) // Cuộn đến vị trí
            }
        }
        radioAdapter.items = list
        binding.rcvRadio.layoutManager =
            ArcLayoutManager(requireActivity(), 0, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvRadio.adapter = radioAdapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rcvRadio)

        // Set ItemDecoration to add overlap/margin between items
        binding.rcvRadio.addItemDecoration(
            OverlapItemDecoration(
                resources.getDimensionPixelSize(R.dimen.item_overlap_no_margin),
                resources.getDimensionPixelSize(R.dimen.item_overlap_no_margin),
                isNewRelease = false
            )
        )

//        binding.rcvRadio.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) { // Kiểm tra nếu cuộn đã hoàn thành
//                    val layoutManager = recyclerView.layoutManager as ArcLayoutManager
//                    val centerView = snapHelper.findSnapView(layoutManager) // Tìm view đang được căn giữa
//
//                    if (centerView != null) {
//                        list[recyclerView.getChildAdapterPosition(centerView)].isSelected = false
//                        for(index in list.indices) {
//                            if(recyclerView.getChildAdapterPosition(centerView) != index) {
//                                list[index].isSelected = false
//                            }
//                        }
//                        radioAdapter.notifyDataSetChanged()
//                    } else {
//                        RecyclerView.NO_POSITION
//                    }
//
//                }
//            }
//        })

        binding.rcvMood.adapter = MoodAndActivityAdapter()
        binding.rcvMood.setOnTouchListener { _, _ -> false }
    }

    override fun onClickView() {

    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentRadioBinding.inflate(inflater)
}