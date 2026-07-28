package com.example.serviceandroid.navigation

import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

/**
 * Registers [OverlayFragmentNavigator] so destinations with tag `<overlay>` can be added
 * on top of the previous screen without destroying its view.
 */
class MusicNavHostFragment : NavHostFragment() {

    override fun onCreateNavController(navController: NavController) {
        navController.navigatorProvider.addNavigator(
            OverlayFragmentNavigator(
                requireContext(),
                childFragmentManager,
                id,
            ),
        )
        super.onCreateNavController(navController)
    }
}
