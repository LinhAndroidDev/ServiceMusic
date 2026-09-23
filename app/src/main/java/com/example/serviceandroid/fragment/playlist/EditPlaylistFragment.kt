package com.example.serviceandroid.fragment.playlist

import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.adapter.EditPlaylistSongAdapter
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.data.playlist.PlaylistMutationResult
import com.example.serviceandroid.databinding.FragmentEditPlaylistBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditPlaylistFragment : BaseFragment<FragmentEditPlaylistBinding>() {
    private val viewModel by viewModels<EditPlaylistViewModel>()
    private val adapter = EditPlaylistSongAdapter()
    private var boundPlaylistId: String? = null
    private var orderDirty = false

    override fun initView() {
        binding.rcvEditSongs.adapter = adapter
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0,
        ) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.1f

            override fun chooseDropTarget(
                selected: RecyclerView.ViewHolder,
                dropTargets: MutableList<RecyclerView.ViewHolder>,
                curX: Int,
                curY: Int,
            ): RecyclerView.ViewHolder? {
                val selectedTop = curY
                val selectedBottom = curY + selected.itemView.height
                var winner: RecyclerView.ViewHolder? = null
                var winnerOverlap = 0
                for (target in dropTargets) {
                    val targetHeight = target.itemView.height
                    if (targetHeight <= 0) continue
                    val overlap = minOf(selectedBottom, target.itemView.bottom) -
                        maxOf(selectedTop, target.itemView.top)
                    if (overlap >= targetHeight / 2 && overlap > winnerOverlap) {
                        winnerOverlap = overlap
                        winner = target
                    }
                }
                return winner
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                orderDirty = true
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    adapter.setDragging(viewHolder, true)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                adapter.setDragging(viewHolder, false)
                super.clearView(recyclerView, viewHolder)
                if (!orderDirty) return
                orderDirty = false
                viewModel.persistOrder(adapter.currentSongs()) { result ->
                    if (result !is PlaylistMutationResult.Success) {
                        (activity as? MainActivity)?.showPlaylistMutation(result)
                    }
                }
            }
        })
        adapter.onStartDrag = { touchHelper.startDrag(it) }
        touchHelper.attachToRecyclerView(binding.rcvEditSongs)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlist.collect { playlist ->
                        if (playlist != null && boundPlaylistId != playlist.id) {
                            boundPlaylistId = playlist.id
                            binding.edtPlaylistName.setText(playlist.title)
                            binding.switchPublic.isChecked = playlist.isPublic
                        }
                    }
                }
                launch {
                    viewModel.songs.collect { songs ->
                        if (!orderDirty) adapter.submit(songs)
                    }
                }
            }
        }
    }

    override fun onClickView() {
        binding.backEditPlaylist.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.savePlaylist.setOnClickListener { savePlaylist() }
    }

    private fun savePlaylist() {
        val title = binding.edtPlaylistName.text?.toString().orEmpty().trim()
        if (title.isBlank()) {
            Toast.makeText(requireContext(), R.string.playlist_name_required, Toast.LENGTH_SHORT)
                .show()
            return
        }
        viewModel.savePlaylist(title, binding.switchPublic.isChecked) { result ->
            if (result is PlaylistMutationResult.Success) {
                Toast.makeText(requireContext(), R.string.playlist_updated, Toast.LENGTH_SHORT)
                    .show()
                findNavController().popBackStack()
            } else {
                (activity as? MainActivity)?.showPlaylistMutation(result)
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentEditPlaylistBinding.inflate(inflater)
}
