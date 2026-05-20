package com.example.serviceandroid.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.serviceandroid.R

fun FragmentActivity.getCurrentFragment(): Fragment? {
    val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as? NavHostFragment
    return navHostFragment?.childFragmentManager?.primaryNavigationFragment
        ?: supportFragmentManager.findFragmentById(R.id.navHostFragment)
}

fun NavController.moveTo(fragmentId: Int) {
    val currentDestination = this.currentDestination
    
    if (currentDestination?.id == fragmentId) {
        return
    }
    
    val popped = this.popBackStack(fragmentId, false)
    
    if (!popped) {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(fragmentId, false)
            .build()
        this.navigate(fragmentId, null, navOptions)
    }
}