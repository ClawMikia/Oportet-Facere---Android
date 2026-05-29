package com.reqsync.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.reqsync.app.R
import com.reqsync.app.adapters.CategorySummaryAdapter
import com.reqsync.app.databinding.FragmentDashboardBinding
import com.reqsync.app.utils.XpUtils
import com.reqsync.app.utils.toXpString
import com.reqsync.app.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var categoryAdapter: CategorySummaryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeState()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategorySummaryAdapter { category ->
            findNavController().navigate(R.id.action_dashboard_to_checklist)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLoading) return@collectLatest

                // Stats
                binding.tvCompletedCount.text = state.completedItems.toString()
                binding.tvPendingCount.text = state.pendingItems.toString()
                binding.tvStreakCount.text = (state.userProgress?.currentStreak ?: 0).toString()

                // Overall progress
                val percent = (state.completionPercent * 100).roundToInt()
                binding.tvCompletionPercent.text = "$percent%"
                binding.progressOverall.progress = percent
                binding.tvProgressSummary.text =
                    "${state.completedItems} of ${state.totalItems} requirements completed"

                // XP bar
                val progress = state.userProgress
                if (progress != null) {
                    val level = progress.level
                    val rank = progress.rank
                    binding.tvRank.text = rank
                    binding.tvLevelLabel.text = "LEVEL $level"
                    val xpPercent = (XpUtils.progressPercent(progress.totalXp) * 100).roundToInt()
                    binding.progressXp.progress = xpPercent
                    val nextLevelXp = XpUtils.xpForNextLevel(level)
                    binding.tvXpLabel.text = "${progress.totalXp.toXpString()} / ${nextLevelXp.toXpString()}"
                }

                // Categories
                categoryAdapter.submitList(state.categories)

                // Empty state
                if (state.categories.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvCategories.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvCategories.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnStartMission.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_paste)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
