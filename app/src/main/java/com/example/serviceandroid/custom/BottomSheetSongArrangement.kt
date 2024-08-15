package com.example.serviceandroid.custom

import android.os.Bundle
import android.view.View
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseBottomSheetDialogFragment

class BottomSheetSongArrangement: BaseBottomSheetDialogFragment() {

    override fun getLayoutId(): Int = R.layout.layout_bottom_sheet_song_arrangement

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Xử lý logic cho BottomSheet này, ví dụ như thiết lập sự kiện click
    }

    override fun onBottomSheetExpanded() {
        super.onBottomSheetExpanded()
        // Logic khi BottomSheet mở rộng
    }

    override fun onBottomSheetCollapsed() {
        super.onBottomSheetCollapsed()
        // Logic khi BottomSheet thu gọn
    }
}