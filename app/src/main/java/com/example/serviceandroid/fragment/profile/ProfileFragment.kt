package com.example.serviceandroid.fragment.profile

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.serviceandroid.R
import androidx.fragment.app.viewModels
import com.example.serviceandroid.adapter.UpdateAccountAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.OverlapItemDecoration
import com.example.serviceandroid.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel by viewModels<ProfileViewModel>()

    override fun initView() {
        binding.header.title.text = "Cá nhân"
        binding.header.viewProfile.isVisible = true
        binding.header.micro.isVisible = false

        initUpdateAccount()
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

    }

    override fun getFragmentBinding(inflater: LayoutInflater)
    = FragmentProfileBinding.inflate(inflater)
}