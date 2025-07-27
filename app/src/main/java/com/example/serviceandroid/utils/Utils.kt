package com.example.serviceandroid.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.serviceandroid.R

fun FragmentActivity.getCurrentFragment(): Fragment {
    val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as? NavHostFragment
    return navHostFragment?.childFragmentManager?.primaryNavigationFragment
        ?: supportFragmentManager.findFragmentById(R.id.navHostFragment) ?: Fragment()
}