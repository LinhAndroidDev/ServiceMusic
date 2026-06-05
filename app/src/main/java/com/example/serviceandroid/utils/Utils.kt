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
    if (currentDestination?.id == fragmentId) {
        return
    }

    // Reuse an existing tab entry when it is still on the back stack.
    if (popBackStack(fragmentId, false)) {
        return
    }

    // Bottom-nav pattern: keep [homeFragment] as root, save/restore tab state instead of
    // clearing the entire stack (avoids cold recreation delay on every tab switch).
    val navOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setRestoreState(true)
        .setPopUpTo(R.id.homeFragment, false, true)
        .build()
    navigate(fragmentId, null, navOptions)
}