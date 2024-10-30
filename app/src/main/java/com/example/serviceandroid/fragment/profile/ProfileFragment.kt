package com.example.serviceandroid.fragment.profile

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.UpdateAccountAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.OverlapItemDecoration
import com.example.serviceandroid.databinding.FragmentProfileBinding
import com.example.serviceandroid.model.UpdateAccount


class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    override fun initView() {
        binding.header.title.text = "Cá nhân"
        binding.header.viewProfile.isVisible = true
        binding.header.micro.isVisible = false

        initUpdateAccount()
    }

    private fun initUpdateAccount() {
        val updateAccounts = mutableListOf(
            UpdateAccount(
                "Plus",
                "19,000đ",
                "Nghe nhạc với chất lượng cao nhất, không \nquảng cáo",
                "Loại bỏ quảng cáo",
                R.drawable.ic_advertisement,
                "Lưu trữ nhạc không giới hạn",
                R.drawable.ic_download_thin,
                "Tuỳ chỉnh chế độ phát nhạc",
                R.drawable.ic_custom,
                R.drawable.bg_purple_corner_10_stroke_1,
                R.color.purple_1
            ),
            UpdateAccount(
                "Premium",
                "49,000đ",
                "Toàn bộ đăc quyền Plus cùng kho nhạc Premium",
                "Nghe và tải tất cả",
                R.drawable.ic_diamond,
                "Loại bỏ quảng cáo",
                R.drawable.ic_advertisement,
                "Lưu trữ nhạc không giới hạn",
                R.drawable.ic_download_thin,
                R.drawable.bg_orange_corner_1,
                R.color.bg_orange
            )
        )
        val updateAccountAdapter = UpdateAccountAdapter(requireActivity())
        updateAccountAdapter.items = updateAccounts
        binding.rcvUpdateAccount.adapter = updateAccountAdapter
        LinearSnapHelper().attachToRecyclerView(binding.rcvUpdateAccount)

        // Set ItemDecoration to add overlap/margin between items
        binding.rcvUpdateAccount.addItemDecoration(
            OverlapItemDecoration(
                resources.getDimensionPixelSize(R.dimen.item_overlap_width),
                resources.getDimensionPixelSize(R.dimen.item_overlap_width),
                isNewRelease = false
            )
        )
    }

    override fun onClickView() {

    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentProfileBinding.inflate(inflater)
}