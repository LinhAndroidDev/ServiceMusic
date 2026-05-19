package com.example.serviceandroid.fragment.profile

import androidx.lifecycle.ViewModel
import com.example.serviceandroid.R
import com.example.serviceandroid.model.UpdateAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    fun getUpdateAccounts(): MutableList<UpdateAccount> = mutableListOf(
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
}
