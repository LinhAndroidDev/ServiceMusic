package com.example.serviceandroid.fragment.singer

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.PagerNewReleaseAdapter
import com.example.serviceandroid.adapter.TypeList
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.custom.BottomSheetOptionMusic
import com.example.serviceandroid.data.artist.FollowMutationResult
import com.example.serviceandroid.databinding.FragmentSingerDetailBinding
import com.example.serviceandroid.fragment.music.MusicPlayerLauncher
import com.example.serviceandroid.playback.PlaybackViewModel
import com.example.serviceandroid.utils.Constant
import com.example.serviceandroid.utils.loadSingerAvatar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class SingerDetailFragment : BaseFragment<FragmentSingerDetailBinding>() {

    private val args by navArgs<SingerDetailFragmentArgs>()
    private val viewModel by viewModels<SingerDetailViewModel>()
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private var songsAdapter: PagerNewReleaseAdapter? = null
    private var descriptionSingerId: String? = null
    private var boundDescription: String? = null
    private var descriptionExpanded = false

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentSingerDetailBinding.inflate(inflater)

    override fun initView() {
        ensureSongsAdapter()
        viewModel.load(args.singerId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressLoading.isVisible = state.isLoading
                    val singer = state.singer
                    binding.btnFollow.isEnabled = singer != null
                    binding.btnPlay.isEnabled = singer != null
                    bindFollowButton(state.isFollowed)
                    if (singer != null) {
                        binding.tvSingerName.text = singer.name
                        binding.imgSingerAvatar.loadSingerAvatar(singer.avatarUrl)
                        bindDescription(singer.id, singer.description.trim())
                    } else if (!state.isLoading && state.error) {
                        binding.tvSingerName.setText(R.string.singer_info_empty)
                        bindDescription(null, "")
                    }

                    binding.tvSongCount.text =
                        getString(R.string.singer_detail_songs_count, state.songs.size)
                    binding.tvEmptySongs.isVisible =
                        !state.isLoading && state.songs.isEmpty()
                    binding.rcvSingerSongs.isVisible = state.songs.isNotEmpty()

                    songsAdapter?.let { adapter ->
                        adapter.items = ArrayList(state.songs)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun ensureSongsAdapter() {
        if (songsAdapter != null) return
        val adapter = PagerNewReleaseAdapter(requireActivity(), TypeList.TYPE_NATIONAL).apply {
            onClickItem = { songId ->
                val songs = viewModel.uiState.value.songs
                if (playbackViewModel.playFromVisibleList(requireContext(), songs, songId)) {
                    MusicPlayerLauncher.open(
                        this@SingerDetailFragment,
                        songId,
                        preservePlayback = true,
                    )
                }
            }
            onClickMoreOption = { song ->
                val dialog = BottomSheetOptionMusic()
                val bundle = Bundle()
                bundle.putParcelable(Constant.KEY_SONG, song)
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "")
            }
        }
        songsAdapter = adapter
        binding.rcvSingerSongs.adapter = adapter
    }

    override fun onClickView() {
        binding.backSingerDetail.setOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnFollow.setOnClickListener { onFollowClicked() }
        binding.btnPlay.setOnClickListener { playSingerSongs() }
    }

    private fun onFollowClicked() {
        val singer = viewModel.uiState.value.singer ?: return
        if (viewModel.uiState.value.isFollowed) {
            viewModel.unfollow(singer.id) { showFollowResult(it, followed = false) }
            return
        }
        (activity as? MainActivity)?.ensureSignedInForArtist {
            val current = viewModel.uiState.value.singer ?: return@ensureSignedInForArtist
            viewModel.follow(current) { showFollowResult(it, followed = true) }
        }
    }

    private fun showFollowResult(result: FollowMutationResult, followed: Boolean) {
        if (!isAdded) return
        when (result) {
            FollowMutationResult.Success -> Toast.makeText(
                requireContext(),
                if (followed) R.string.artist_followed_toast else R.string.artist_unfollowed_toast,
                Toast.LENGTH_SHORT,
            ).show()
            FollowMutationResult.RequiresLogin -> Toast.makeText(
                requireContext(),
                R.string.artist_login_message,
                Toast.LENGTH_LONG,
            ).show()
            FollowMutationResult.Offline -> Toast.makeText(
                requireContext(),
                R.string.artist_offline,
                Toast.LENGTH_LONG,
            ).show()
            is FollowMutationResult.Failure -> Toast.makeText(
                requireContext(),
                result.message.ifBlank { getString(R.string.artist_operation_failed) },
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun playSingerSongs() {
        val songs = viewModel.uiState.value.songs
        val first = songs.firstOrNull()
        if (first == null) {
            Toast.makeText(requireContext(), R.string.artist_play_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (playbackViewModel.playFromVisibleList(requireContext(), songs, first.id)) {
            MusicPlayerLauncher.open(this, first.id, preservePlayback = true)
        }
    }

    private fun bindFollowButton(followed: Boolean) {
        binding.btnFollow.setText(if (followed) R.string.artist_following else R.string.artist_follow)
        binding.btnFollow.setBackgroundResource(
            if (followed) R.drawable.bg_corner_25_purple else R.drawable.bg_corner_25_stroke_grey_1,
        )
        binding.btnFollow.setTextColor(
            ContextCompat.getColor(requireContext(), if (followed) R.color.white else R.color.text_black),
        )
    }

    private fun bindDescription(singerId: String?, description: String) {
        val show = description.isNotEmpty()
        if (descriptionSingerId == singerId && boundDescription == description) return
        binding.tvSingerInfoTitle.isVisible = show
        binding.tvSingerDescription.isVisible = show
        descriptionSingerId = singerId
        boundDescription = description
        descriptionExpanded = false
        if (!show) return
        showCollapsedDescription(description)
    }

    private fun showCollapsedDescription(full: String) {
        val textView = binding.tvSingerDescription
        textView.maxLines = Int.MAX_VALUE
        textView.text = full
        textView.movementMethod = null
        textView.post {
            if (!isAdded || descriptionExpanded || descriptionSingerId == null) return@post
            val layout = textView.layout ?: return@post
            if (layout.lineCount <= COLLAPSED_LINES) return@post
            val more = getString(R.string.artist_see_more)
            val suffix = "… $more"
            val width = (textView.width - textView.paddingLeft - textView.paddingRight).coerceAtLeast(0)
            val lineStart = layout.getLineStart(COLLAPSED_LINES - 1)
            var end = layout.getLineEnd(COLLAPSED_LINES - 1).coerceAtMost(full.length)
            var prefix = full.substring(0, end).trimEnd()
            while (prefix.length > lineStart &&
                textView.paint.measureText(prefix.substring(lineStart.coerceAtMost(prefix.length)) + suffix) > width
            ) {
                prefix = prefix.dropLast(1).trimEnd()
            }
            val visible = prefix.trimEnd() + suffix
            textView.maxLines = COLLAPSED_LINES
            textView.text = linkSuffix(visible, more) {
                descriptionExpanded = true
                showExpandedDescription(full)
            }
            textView.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun showExpandedDescription(full: String) {
        val less = getString(R.string.artist_see_less)
        val visible = "$full $less"
        val textView = binding.tvSingerDescription
        textView.maxLines = Int.MAX_VALUE
        textView.text = linkSuffix(visible, less) {
            descriptionExpanded = false
            showCollapsedDescription(full)
        }
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun linkSuffix(text: String, label: String, onClick: () -> Unit): SpannableString {
        val spannable = SpannableString(text)
        val start = (text.length - label.length).coerceAtLeast(0)
        val color = ContextCompat.getColor(requireContext(), R.color.purple_1)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) = onClick()

            override fun updateDrawState(ds: TextPaint) {
                ds.color = color
                ds.isUnderlineText = false
            }
        }, start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        (binding.tvSingerDescription as TextView).highlightColor = Color.TRANSPARENT
        return spannable
    }

    private companion object {
        const val COLLAPSED_LINES = 3
    }
}
