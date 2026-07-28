package com.example.serviceandroid.navigation

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider

/**
 * Adds the destination on top of the current screen without [FragmentTransaction.replace].
 *
 * Does **not** use [FragmentManager.addToBackStack] so the default [androidx.navigation.fragment.FragmentNavigator]
 * does not see these commits (which would otherwise crash with "unknown to the FragmentNavigator").
 * Back stack is owned by NavController via [state].
 */
@Navigator.Name(OverlayFragmentNavigator.NAME)
class OverlayFragmentNavigator(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
) : Navigator<OverlayFragmentNavigator.Destination>() {

    override fun createDestination(): Destination = Destination(this)

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        if (fragmentManager.isStateSaved) {
            Log.i(TAG, "Ignoring navigate(): FragmentManager already saved state")
            return
        }
        for (entry in entries) {
            navigate(entry, navOptions)
        }
    }

    private fun navigate(entry: NavBackStackEntry, navOptions: NavOptions?) {
        val destination = entry.destination as Destination
        var className = destination.className
        if (className[0] == '.') {
            className = context.packageName + className
        }
        val fragment = fragmentManager.fragmentFactory.instantiate(context.classLoader, className)
        fragment.arguments = entry.arguments

        val ft = fragmentManager.beginTransaction()
        applyEnterAnimations(ft, navOptions)
        // Keep previous fragment view; do not replace/hide or use FM back stack.
        ft.add(containerId, fragment, entry.id)
        ft.setPrimaryNavigationFragment(fragment)
        ft.setReorderingAllowed(true)
        // commit() — never commitNow(): pop can run while FragmentManager is already executing
        // (e.g. Activity.onBackPressed → FM → NavController → this navigator).
        ft.commit()
        state.push(entry)
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        if (fragmentManager.isStateSaved) {
            Log.i(TAG, "Ignoring popBackStack(): FragmentManager already saved state")
            return
        }
        val fragment = fragmentManager.findFragmentByTag(popUpTo.id)
        // Update NavController state first so back stack stays consistent.
        state.pop(popUpTo, savedState)
        if (fragment == null) return

        val ft = fragmentManager.beginTransaction()
        // Swipe-dismiss already animated the sheet; keep remove instant.
        ft.setCustomAnimations(0, 0)
        ft.remove(fragment)
        restorePrimaryNavigationFragment(ft, excluding = fragment)
        ft.setReorderingAllowed(true)
        ft.commit()
    }

    private fun restorePrimaryNavigationFragment(
        ft: FragmentTransaction,
        excluding: Fragment,
    ) {
        val previous = fragmentManager.fragments
            .asReversed()
            .firstOrNull { it != excluding && it.isAdded && !it.isRemoving }
        if (previous != null) {
            ft.setPrimaryNavigationFragment(previous)
        }
    }

    private fun applyEnterAnimations(ft: FragmentTransaction, navOptions: NavOptions?) {
        if (navOptions == null) return
        var enterAnim = navOptions.enterAnim
        var exitAnim = navOptions.exitAnim
        var popEnterAnim = navOptions.popEnterAnim
        var popExitAnim = navOptions.popExitAnim
        if (enterAnim != -1 || exitAnim != -1 || popEnterAnim != -1 || popExitAnim != -1) {
            enterAnim = if (enterAnim != -1) enterAnim else 0
            exitAnim = if (exitAnim != -1) exitAnim else 0
            popEnterAnim = if (popEnterAnim != -1) popEnterAnim else 0
            popExitAnim = if (popExitAnim != -1) popExitAnim else 0
            ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
        }
    }

    @NavDestination.ClassType(Fragment::class)
    class Destination : NavDestination {
        constructor(navigator: Navigator<out Destination>) : super(navigator)
        constructor(provider: NavigatorProvider) :
            this(provider.getNavigator(OverlayFragmentNavigator::class.java))

        private var _className: String? = null

        val className: String
            get() = checkNotNull(_className) { "Fragment class was not set" }

        fun setClassName(className: String): Destination {
            _className = className
            return this
        }

        @CallSuper
        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            val className = attrs.getAttributeValue(ANDROID_NS, "name")
            if (!className.isNullOrBlank()) {
                setClassName(className)
            }
        }
    }

    companion object {
        const val NAME = "overlay"
        private const val TAG = "OverlayFragmentNav"
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
