package com.reqsync.app.ui.checklist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.reqsync.app.R
import com.reqsync.app.adapters.ChecklistAdapter
import com.reqsync.app.data.database.entities.RequirementStatus
import com.reqsync.app.databinding.FragmentChecklistBinding
import com.reqsync.app.ui.MainActivity
import com.reqsync.app.viewmodels.ChecklistViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChecklistViewModel by viewModels()
    private lateinit var checklistAdapter: ChecklistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupSearch()
        observeState()
        observeXpEvents()
    }

    private fun setupRecyclerView() {
        checklistAdapter = ChecklistAdapter(
            onItemChecked = { item -> viewModel.toggleItemStatus(item) },
            onItemClicked = { item ->
                val bundle = android.os.Bundle().apply { putLong("itemId", item.id) }
                findNavController().navigate(R.id.action_checklist_to_details, bundle)
            },
            onCategoryToggled = { category, expanded ->
                viewModel.toggleCategoryExpanded(category.id, expanded)
            }
        )
        binding.rvChecklist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = checklistAdapter
            itemAnimator = null
        }

        // Attach swipe-to-delete
        val swipeCallback = com.reqsync.app.utils.SwipeToDeleteCallback(requireContext()) { position ->
            val item = checklistAdapter.currentList.getOrNull(position)
            if (item is ChecklistAdapter.ListItem.RequirementRow) {
                viewModel.deleteItem(item.item)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvChecklist)
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilter(null)
            uncheckOthers("all")
        }
        binding.chipPending.setOnClickListener {
            viewModel.setFilter(RequirementStatus.PENDING)
            uncheckOthers("pending")
        }
        binding.chipCompleted.setOnClickListener {
            viewModel.setFilter(RequirementStatus.COMPLETED)
            uncheckOthers("completed")
        }
        binding.chipOverdue.setOnClickListener {
            viewModel.setFilter(RequirementStatus.OVERDUE)
            uncheckOthers("overdue")
        }
    }

    private fun uncheckOthers(selected: String) {
        binding.chipAll.isChecked = selected == "all"
        binding.chipPending.isChecked = selected == "pending"
        binding.chipCompleted.isChecked = selected == "completed"
        binding.chipOverdue.isChecked = selected == "overdue"
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLoading) return@collectLatest

                // Compute per-category stats
                val allItems = state.itemsByCategory.values.flatten()
                checklistAdapter.statsMap = com.reqsync.app.utils.CategoryProgressHelper
                    .computeStats(state.categories, allItems)

                // Build flat list: category header + items for each category
                val listItems = mutableListOf<ChecklistAdapter.ListItem>()
                state.categories.forEach { category ->
                    listItems.add(ChecklistAdapter.ListItem.CategoryHeader(category))
                    if (category.isExpanded) {
                        val items = state.itemsByCategory[category.id] ?: emptyList()
                        items.forEach { item ->
                            listItems.add(ChecklistAdapter.ListItem.RequirementRow(item))
                        }
                    }
                }

                checklistAdapter.submitList(listItems)

                // Empty state
                val hasContent = state.categories.isNotEmpty()
                binding.rvChecklist.visibility = if (hasContent) View.VISIBLE else View.GONE
                binding.layoutEmpty.visibility = if (!hasContent) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeXpEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.xpEvent.collectLatest { xp ->
                if (xp != null) {
                    (activity as? MainActivity)?.showXpGain(xp)
                    viewModel.consumeXpEvent()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
