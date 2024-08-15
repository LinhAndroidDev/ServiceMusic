package com.example.serviceandroid.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Gán layout từ lớp con
        return inflater.inflate(getLayoutId(), container, false)
    }

    // Phương thức trừu tượng để lớp con cung cấp layout
    abstract fun getLayoutId(): Int

    override fun onStart() {
        super.onStart()
        // Tuỳ chọn: Thay đổi chiều cao của BottomSheet nếu cần
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { view ->
            view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    // Các phương thức tiện ích có thể override ở lớp con nếu cần
    open fun onBottomSheetExpanded() {
        // Thực hiện điều gì đó khi BottomSheet mở rộng
    }

    open fun onBottomSheetCollapsed() {
        // Thực hiện điều gì đó khi BottomSheet thu gọn
    }

    open fun onBottomSheetHidden() {
        // Thực hiện điều gì đó khi BottomSheet bị ẩn
    }
}