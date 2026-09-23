package com.example.serviceandroid.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.serviceandroid.fragment.SearchSingerResultsFragment
import com.example.serviceandroid.fragment.SearchSongResultsFragment

class SearchResultsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment =
        if (position == PAGE_SONGS) SearchSongResultsFragment()
        else SearchSingerResultsFragment()

    companion object {
        const val PAGE_SONGS = 0
        const val PAGE_SINGERS = 1
        const val PAGE_COUNT = 2
    }
}
