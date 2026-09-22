package com.example.serviceandroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val binding by lazy { FragmentSplashBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<SplashViewModel>()

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateHome.collect {
                    findNavController().navigate(R.id.homeFragment)
                }
            }
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

    override fun onResume() {
        super.onResume()
        viewModel.restartSplashTimer()
    }

    override fun onStop() {
        viewModel.cancelSplashTimer()
        super.onStop()
    }
}
