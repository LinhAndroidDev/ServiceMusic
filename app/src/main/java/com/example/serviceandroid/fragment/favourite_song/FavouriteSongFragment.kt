package com.example.serviceandroid.fragment.favourite_song

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentFavouriteSongBinding
import com.example.serviceandroid.helper.Data

class FavouriteSongFragment : BaseFragment<FragmentFavouriteSongBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL)
        adapter.isFavourite = true
        adapter.items = Data.listMusic()
        binding.rcvFavouriteSong.adapter = adapter
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
            = FragmentFavouriteSongBinding.inflate(inflater)
}