package com.example.serviceandroid.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment<VD: ViewDataBinding> : BottomSheetDialogFragment(), CoreInterface.AndroidView {
    protected lateinit var binding: VD

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Gán layout từ lớp con
        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        initView()
        onClickView()
        return binding.root
    }

    // Phương thức trừu tượng để lớp con cung cấp layout
    abstract val layoutResId: Int

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