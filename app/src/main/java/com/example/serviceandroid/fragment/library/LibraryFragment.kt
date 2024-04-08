package com.example.serviceandroid.fragment.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.LibraryAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentLibraryBinding
import com.example.serviceandroid.model.Library

class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Thư viện"
        initLibrary()
    }

    private fun initLibrary() {
        val librarys = arrayListOf(
            Library(R.drawable.favourite, "Bài hát yêu thích", 153, R.color.bg_blue),
            Library(R.drawable.ic_download, "Đã tải", 0, R.color.bg_purple),
            Library(R.drawable.ic_artist, "Nghệ sĩ", 0, R.color.bg_orange),
            Library(R.drawable.ic_upload, "Upload", 0, R.color.yellow_dark),
            Library(R.drawable.ic_mv, "MV", 0, R.color.bg_purple),
        )
        val libraryAdapter = LibraryAdapter(requireActivity())
        libraryAdapter.items = librarys
        binding.rcvLibrary.adapter = libraryAdapter
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentLibraryBinding.inflate(inflater)

}

