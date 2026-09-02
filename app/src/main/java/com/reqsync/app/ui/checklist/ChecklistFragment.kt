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
import com.reqsync.app.viewmodels.DialogEvent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    override fun onResume() {
        super.onResume()
        // When this tab is (re)shown after switching away, the Chip widgets restore
        // their previously checked state from the saved instance state even though the
        // fragment-scoped ViewModel starts fresh (unfiltered). Reset the filter chips
        // and search to their default so the user sees the full batch.
        viewModel.setFilter(null)
        viewModel.setSearchQuery("")
        uncheckOthers("all")
        binding.etSearch.setText("")
    }

    private fun setupRecyclerView() {
        binding.rvChecklist.layoutManager = LinearLayoutManager(requireContext())
        // Vertical slide for expand/collapse of sub items under a parent header.
        binding.rvChecklist.itemAnimator = com.reqsync.app.utils.VerticalSlideItemAnimator()
        // Attach swipe-to-delete once (survives adapter swaps)
        val swipeCallback = com.reqsync.app.utils.SwipeToDeleteCallback(requireContext()) { position ->
            val item = checklistAdapter.currentList.getOrNull(position)
            if (item is ChecklistAdapter.ListItem.RequirementRow) {
                viewModel.deleteItem(item.item)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvChecklist)
        rebuildAdapter()
    }

    // Create a brand-new adapter so the RecyclerView drops every previously-attached
    // item view. This guarantees the old items disappear instead of lingering through
    // the diff/animator.
    private fun rebuildAdapter() {
        checklistAdapter = ChecklistAdapter(
            onItemChecked = { item -> viewModel.toggleItemStatus(item) },
            onItemClicked = { item ->
                val bundle = android.os.Bundle().apply { putLong("itemId", item.id) }
                findNavController().navigate(R.id.action_checklist_to_details, bundle)
            },
            onItemArchived = { item ->
                viewModel.archiveItem(item.id)
            },
            onCategoryToggled = { category, expanded ->
                viewModel.toggleCategoryExpanded(category.id, expanded)
            },
            onCategoryArchived = { category ->
                viewModel.archiveCategory(category.id)
            }
        )
        binding.rvChecklist.adapter = checklistAdapter
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilter(null)
            uncheckOthers("all")
            rebuildAdapter()
            refreshList(null)
        }
        binding.chipPending.setOnClickListener {
            viewModel.setFilter(RequirementStatus.PENDING)
            uncheckOthers("pending")
            rebuildAdapter()
            refreshList(RequirementStatus.PENDING)
        }
        binding.chipCompleted.setOnClickListener {
            viewModel.setFilter(RequirementStatus.COMPLETED)
            uncheckOthers("completed")
            rebuildAdapter()
            refreshList(RequirementStatus.COMPLETED)
        }
        binding.chipOverdue.setOnClickListener {
            viewModel.setFilter(RequirementStatus.OVERDUE)
            uncheckOthers("overdue")
            rebuildAdapter()
            refreshList(RequirementStatus.OVERDUE)
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

                // Handle dialog events
                state.dialogEvent?.let { event ->
                    showAestheticDialog(event)
                    viewModel.consumeDialogEvent()
                }

                // Use pre-computed stats from ViewModel (Request 1 fix)
                // Note: stats are bundled into CategoryHeader ListItems so the adapter's
                // DiffUtil can reliably detect and rebind header changes (avoids the
                // notifyDataSetChanged + submitList conflict that broke expand/collapse).

                refreshList(state.filterStatus)
            }
        }
    }

    // Rebuild the flat adapter list from the current ViewModel state *and* submit it.
    // Filter + search are applied here locally. On chip taps a fresh adapter is used
    // (see rebuildAdapter) so the previous items are gone before this submits.
    private fun refreshList(filter: RequirementStatus?) {
        val state = viewModel.uiState.value
        val query = state.searchQuery

        // Filter active items by the selected chip + search, then build the flat list.
        val filteredByCategory = state.itemsByCategory.mapValues { (_, items) ->
            items.filter { item ->
                val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true)
                val matchesFilter = filter == null || item.status == filter
                matchesQuery && matchesFilter
            }
        }

        val listItems = mutableListOf<ChecklistAdapter.ListItem>()
        state.categories.forEach { category ->
            val stats = state.categoryStats[category.id]
            val totalInCat = stats?.total ?: 0
            val filtered = filteredByCategory[category.id] ?: emptyList()

            // Flattening logic: Only flatten if it's NOT the "Others" category
            // because the user specifically asked for "Others" to be a category.
            val isOthers = category.title.startsWith("Others - ")

            if (totalInCat == 1 && !isOthers && filtered.isNotEmpty()) {
                // Show the single matching item directly, skipping the header
                listItems.add(ChecklistAdapter.ListItem.RequirementRow(filtered[0]))
            } else if (totalInCat >= 1) {
                // Multi-item category or single-item "Others": show header then items if expanded
                listItems.add(ChecklistAdapter.ListItem.CategoryHeader(category, stats))
                if (category.isExpanded) {
                    filtered.forEach { item ->
                        listItems.add(ChecklistAdapter.ListItem.RequirementRow(item))
                    }
                }
            } else {
                // Category with 0 items: Show header so user can see/manage it.
                if (query.isBlank() && filter == null) {
                    listItems.add(ChecklistAdapter.ListItem.CategoryHeader(category, stats))
                }
            }
        }

        checklistAdapter.submitList(listItems)

        // Empty state
        val hasContent = listItems.isNotEmpty()
        binding.rvChecklist.visibility = if (hasContent) View.VISIBLE else View.GONE
        binding.layoutEmpty.visibility = if (!hasContent) View.VISIBLE else View.GONE
    }

    private fun showAestheticDialog(event: DialogEvent) {
        val (title, message) = when (event) {
            is DialogEvent.Archived -> Pair(
                "MISSION ARCHIVED",
                "\"${event.title}\" has been moved to the archives."
            )
            is DialogEvent.CategoryCompleted -> Pair(
                "MISSION ACCOMPLISHED",
                "All requirements in \"${event.categoryName}\" are now complete."
            )
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.CardStyle_Cyberpunk)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("CONFIRMED", null)
            .show()
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
