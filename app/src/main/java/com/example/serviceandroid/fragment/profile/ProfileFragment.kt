package com.example.serviceandroid.fragment.profile

import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
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
import com.example.serviceandroid.databinding.FragmentProfileBinding
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel by viewModels<ProfileViewModel>()
    private val credentialManager by lazy { CredentialManager.create(requireContext()) }

    override fun initView() {
        binding.header.title.text = "Cá nhân"
        binding.header.viewProfile.isVisible = true
        binding.header.micro.isVisible = false

        initUpdateAccount()
        observeAuthState()
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
        val serverClientId = getDefaultWebClientId()
        if (serverClientId.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                R.string.auth_config_missing,
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = credentialManager.getCredential(
                    context = requireContext(),
                    request = request,
                )
                val credential = response.credential
                if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.signInWithGoogle(googleCredential.idToken)
                } else {
                    showToast(R.string.auth_invalid_credential)
                }
            } catch (_: GetCredentialCancellationException) {
                // Người dùng chủ động đóng màn hình chọn tài khoản.
            } catch (_: GetCredentialException) {
                showToast(R.string.auth_google_unavailable)
            } catch (_: Exception) {
                showToast(R.string.auth_invalid_credential)
            }
        }
    }

    private fun getDefaultWebClientId(): String? {
        val resourceId = resources.getIdentifier(
            "default_web_client_id",
            "string",
            requireContext().packageName,
        )
        return resourceId.takeIf { it != 0 }?.let(resources::getString)
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
    }
}