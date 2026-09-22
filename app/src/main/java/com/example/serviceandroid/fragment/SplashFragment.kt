package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.serviceandroid.R
import com.example.serviceandroid.ads.AppOpenAdController
import com.example.serviceandroid.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val binding by lazy { FragmentSplashBinding.inflate(layoutInflater) }

    @Inject
    lateinit var appOpenAdController: AppOpenAdController

    private var adFlowStarted = false
    private var leavingSplash = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playEntrance()
        beginAdFlow()
    }

    private fun beginAdFlow() {
        if (adFlowStarted || leavingSplash) return
        adFlowStarted = true
        appOpenAdController.load { loaded ->
            if (!isAdded || leavingSplash) return@load
            leavingSplash = true
            val activity = requireActivity()
            goHome()
            if (!loaded) return@load
            parentFragmentManager.executePendingTransactions()
            appOpenAdController.showIfAvailable(activity) {}
        }
    }

    private fun playEntrance() {
        val interpolator = DecelerateInterpolator()
        binding.splashGlow.alpha = 0f
        binding.splashMark.alpha = 0f
        binding.splashMark.scaleX = 0.86f
        binding.splashMark.scaleY = 0.86f
        binding.splashTitle.alpha = 0f
        binding.splashTitle.translationY = 16f
        binding.splashSubtitle.alpha = 0f

        binding.splashGlow.animate()
            .alpha(1f)
            .setDuration(700L)
            .setInterpolator(interpolator)
            .start()
        binding.splashMark.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(520L)
            .setInterpolator(interpolator)
            .start()
        binding.splashTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(160L)
            .setDuration(420L)
            .setInterpolator(interpolator)
            .start()
        binding.splashSubtitle.animate()
            .alpha(1f)
            .setStartDelay(280L)
            .setDuration(420L)
            .setInterpolator(interpolator)
            .start()
    }

    private fun goHome() {
        if (!isAdded) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.splashFragment) return
        navController.navigate(
            R.id.homeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.splashFragment, true)
                .setEnterAnim(0)
                .setExitAnim(0)
                .setPopEnterAnim(0)
                .setPopExitAnim(0)
                .build(),
        )
    }

}
