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
import com.example.serviceandroid.utils.ExtensionFunctions.setColorTint

class RadioFragment : BaseFragment<FragmentRadioBinding>() {

    @SuppressLint("ClickableViewAccessibility", "ResourceAsColor")
    override fun initView() {
        binding.header.title.text = "Radio"
        binding.header.title.setTextColor(requireActivity().getColor(R.color.white))
        setColorTint(binding.header.search, R.color.white)
        setColorTint(binding.header.micro, R.color.white)
        val radioAdapter = RadioAdapter()
        binding.rcvRadio.layoutManager =
            ArcLayoutManager(requireActivity(), 0, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvRadio.adapter = radioAdapter
        LinearSnapHelper().attachToRecyclerView(binding.rcvRadio)

        // Set ItemDecoration to add overlap/margin between items
        binding.rcvRadio.addItemDecoration(
            OverlapItemDecoration(
                resources.getDimensionPixelSize(R.dimen.item_overlap_no_margin),
                resources.getDimensionPixelSize(R.dimen.item_overlap_no_margin),
                isNewRelease = false
            )
        )

        binding.rcvMood.adapter = MoodAndActivityAdapter()
        binding.rcvMood.setOnTouchListener { _, _ -> false }
    }

    override fun onClickView() {

    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentRadioBinding.inflate(inflater)
}