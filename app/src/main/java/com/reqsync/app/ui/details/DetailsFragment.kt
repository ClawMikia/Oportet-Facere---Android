package com.reqsync.app.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.reqsync.app.adapters.NoteAdapter
import com.reqsync.app.data.database.entities.RequirementStatus
import com.reqsync.app.databinding.FragmentDetailsBinding
import com.reqsync.app.utils.toFormattedDate
import com.reqsync.app.viewmodels.DetailsViewModel
import com.reqsync.app.viewmodels.DialogEvent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailsViewModel by viewModels()
    private lateinit var noteAdapter: NoteAdapter

    // Read itemId from bundle
    private val itemId: Long by lazy {
        arguments?.getLong("itemId", -1L) ?: -1L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setItemId(itemId)

        setupRecyclerView()
        observeItem()
        observeNotes()
        observeDialogs()
        setupClickListeners()
    }

    private fun observeDialogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dialogEvent.collectLatest { event ->
                event?.let {
                    if (it is DialogEvent.CategoryCompleted) {
                        MaterialAlertDialogBuilder(requireContext(), com.reqsync.app.R.style.CardStyle_Cyberpunk)
                            .setTitle("MISSION ACCOMPLISHED")
                            .setMessage("All requirements in \"${it.categoryName}\" are now complete.")
                            .setPositiveButton("CONFIRMED", null)
                            .show()
                    }
                    viewModel.consumeDialogEvent()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter { note -> viewModel.deleteNote(note) }
        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter
        }
    }

    private fun observeItem() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.item.collectLatest { item ->
                if (item == null) return@collectLatest

                binding.tvItemTitle.text = item.title
                binding.tvXpReward.text = "+${item.xpReward} XP on completion"
                binding.tvPriority.text = item.priority.name
                binding.tvOptional.text = if (item.isOptional) "Yes" else "No"
                binding.tvCreatedAt.text = item.createdAt.toFormattedDate()

                // Status badge
                when (item.status) {
                    RequirementStatus.COMPLETED -> {
                        binding.tvStatusBadge.text = "COMPLETED"
                        binding.tvStatusBadge.setTextColor(resources.getColor(com.reqsync.app.R.color.status_completed, null))
                        binding.tvStatusBadge.setBackgroundResource(com.reqsync.app.R.drawable.bg_status_completed)
                        binding.btnMarkComplete.isEnabled = false
                        binding.btnMarkComplete.alpha = 0.5f
                        // Update btn_in_progress when marked as complete
                        binding.btnInProgress.text = "COMPLETED"
                        binding.btnInProgress.setTextColor(resources.getColor(com.reqsync.app.R.color.status_completed, null))
                        binding.btnInProgress.strokeColor = resources.getColorStateList(com.reqsync.app.R.color.status_completed, null)
                        // Show completed at
                        item.completedAt?.let { ts ->
                            binding.layoutCompletedAt.visibility = View.VISIBLE
                            binding.tvCompletedAt.text = ts.toFormattedDate()
                        }
                    }
                    RequirementStatus.OVERDUE -> {
                        binding.tvStatusBadge.text = "OVERDUE"
                        binding.tvStatusBadge.setTextColor(resources.getColor(com.reqsync.app.R.color.status_overdue, null))
                        binding.tvStatusBadge.setBackgroundResource(com.reqsync.app.R.drawable.bg_status_overdue)
                    }
                    RequirementStatus.IN_PROGRESS -> {
                        binding.tvStatusBadge.text = "IN PROGRESS"
                        binding.tvStatusBadge.setTextColor(resources.getColor(com.reqsync.app.R.color.status_in_progress, null))
                        // Reset btn_in_progress when in progress
                        binding.btnInProgress.text = "IN PROGRESS"
                        binding.btnInProgress.setTextColor(resources.getColor(com.reqsync.app.R.color.electric_blue, null))
                        binding.btnInProgress.strokeColor = resources.getColorStateList(com.reqsync.app.R.color.electric_blue, null)
                    }
                    else -> {
                        binding.tvStatusBadge.text = "PENDING"
                        binding.tvStatusBadge.setTextColor(resources.getColor(com.reqsync.app.R.color.status_pending, null))
                        binding.tvStatusBadge.setBackgroundResource(com.reqsync.app.R.drawable.bg_status_pending)
                        // Reset btn_in_progress when pending
                        binding.btnInProgress.text = "IN PROGRESS"
                        binding.btnInProgress.setTextColor(resources.getColor(com.reqsync.app.R.color.electric_blue, null))
                        binding.btnInProgress.strokeColor = resources.getColorStateList(com.reqsync.app.R.color.electric_blue, null)
                    }
                }
            }
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collectLatest { notes ->
                noteAdapter.submitList(notes)
                binding.tvNoNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnMarkComplete.setOnClickListener {
            viewModel.markComplete()
        }

        binding.btnInProgress.setOnClickListener {
            viewModel.updateStatus(RequirementStatus.IN_PROGRESS)
        }

        binding.btnSaveNote.setOnClickListener {
            val noteText = binding.etNote.text.toString().trim()
            if (noteText.isNotBlank()) {
                viewModel.addNote(noteText)
                binding.etNote.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
