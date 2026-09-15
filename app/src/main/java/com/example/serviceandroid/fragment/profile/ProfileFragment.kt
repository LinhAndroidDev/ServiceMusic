package com.example.serviceandroid.fragment.profile

import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.UpdateAccountAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.OverlapItemDecoration
import com.example.serviceandroid.data.auth.AuthUser
import com.example.serviceandroid.data.auth.GoogleSignInHelper
import com.example.serviceandroid.data.auth.GoogleSignInRequestResult
import com.example.serviceandroid.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel by viewModels<ProfileViewModel>()

    override fun initView() {
        binding.header.title.text = "Cá nhân"
        binding.header.viewProfile.isVisible = true
        binding.header.micro.isVisible = false

        initUpdateAccount()
        observeAuthState()
        observeProfileEvents()
    }

    private fun initUpdateAccount() {
        val updateAccounts = viewModel.getUpdateAccounts()
        val updateAccountAdapter = UpdateAccountAdapter(requireActivity())
        updateAccountAdapter.items = updateAccounts
        binding.rcvUpdateAccount.adapter = updateAccountAdapter
        LinearSnapHelper().attachToRecyclerView(binding.rcvUpdateAccount)

        // Set ItemDecoration to add overlap/margin between items
        binding.rcvUpdateAccount.addItemDecoration(
            OverlapItemDecoration(
                resources.getDimensionPixelSize(R.dimen.item_overlap_width),
                resources.getDimensionPixelSize(R.dimen.item_overlap_width),
                isNewRelease = false
            )
        )
    }

    override fun onClickView() {
        binding.buttonAuth.setOnClickListener {
            if (viewModel.uiState.value.user == null) {
                openGoogleAccountChooser()
            } else {
                viewModel.signOut()
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentProfileBinding.inflate(inflater)

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUser(state.user)
                    binding.authProgress.isVisible = state.isLoading
                    binding.buttonAuth.isEnabled = !state.isLoading
                    binding.accountContainer.alpha = if (state.isLoading) 0.55f else 1f

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun observeProfileEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        ProfileEvent.AskToSyncLocalHistory -> showHistorySyncDialog()
                        ProfileEvent.LocalHistorySynced ->
                            showToast(R.string.recent_history_sync_success)
                        ProfileEvent.LocalHistoryDiscarded ->
                            showToast(R.string.recent_history_discarded)
                    }
                }
            }
        }
    }

    private fun showHistorySyncDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recent_history_sync_title)
            .setMessage(R.string.recent_history_sync_message)
            .setPositiveButton(R.string.recent_history_sync_action) { _, _ ->
                viewModel.syncLocalHistory()
            }
            .setNegativeButton(R.string.recent_history_discard_action) { _, _ ->
                viewModel.discardLocalHistory()
            }
            .setCancelable(false)
            .show()
    }

    private fun renderUser(user: AuthUser?) {
        (activity as? MainActivity)?.updateProfileTabAvatar(user)

        if (user == null) {
            binding.profileName.setText(R.string.profile_signed_out_title)
            binding.profileEmail.setText(R.string.profile_signed_out_subtitle)
            binding.buttonAuth.setText(R.string.profile_sign_in_google)
            Glide.with(this)
                .load(R.drawable.img_avatar)
                .into(binding.profileAvatar)
            return
        }

        binding.profileName.text = user.displayName.ifBlank {
            getString(R.string.profile_user_fallback)
        }
        binding.profileEmail.text = user.email
        binding.buttonAuth.setText(R.string.profile_sign_out)
        Glide.with(this)
            .load(user.photoUrl)
            .placeholder(R.drawable.img_avatar)
            .error(R.drawable.img_avatar)
            .into(binding.profileAvatar)
    }

    private fun openGoogleAccountChooser() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = GoogleSignInHelper.requestIdToken(requireContext())) {
                is GoogleSignInRequestResult.IdToken ->
                    viewModel.signInWithGoogle(result.value)
                is GoogleSignInRequestResult.Error -> showToast(result.messageRes)
                GoogleSignInRequestResult.Cancelled -> Unit
            }
        }
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
    }
}