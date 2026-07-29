package com.example.serviceandroid.fragment.music

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

object MusicPlayerLauncher {

    fun open(
        activity: FragmentActivity,
        songId: String,
        preservePlayback: Boolean = false,
    ) {
        open(activity.supportFragmentManager, songId, preservePlayback)
    }

    fun open(
        fragment: Fragment,
        songId: String,
        preservePlayback: Boolean = false,
    ) {
        open(fragment.requireActivity().supportFragmentManager, songId, preservePlayback)
    }

    fun open(
        fragmentManager: FragmentManager,
        songId: String,
        preservePlayback: Boolean = false,
    ) {
        val existing = fragmentManager.findFragmentByTag(FragmentMusic.TAG) as? FragmentMusic
        if (existing != null) {
            if (existing.dialog?.isShowing == true) {
                existing.playSongIfNeeded(songId, preservePlayback)
                return
            }
            // Fragment still in FM but dialog dismissed — remove before showing a fresh instance.
            existing.dismissAllowingStateLoss()
        }
        FragmentMusic.newInstance(songId, preservePlayback)
            .show(fragmentManager, FragmentMusic.TAG)
    }

    fun find(fragmentManager: FragmentManager): FragmentMusic? =
        fragmentManager.findFragmentByTag(FragmentMusic.TAG) as? FragmentMusic
}
