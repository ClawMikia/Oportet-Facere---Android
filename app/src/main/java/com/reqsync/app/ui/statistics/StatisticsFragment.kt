package com.reqsync.app.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.reqsync.app.adapters.CategoryStatAdapter
import com.reqsync.app.databinding.FragmentStatisticsBinding
import com.reqsync.app.utils.XpUtils
import com.reqsync.app.utils.toXpString
import com.reqsync.app.viewmodels.StatisticsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()
    private lateinit var categoryStatAdapter: CategoryStatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryStatAdapter = CategoryStatAdapter()
        binding.rvCategoryStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoryStats.adapter = categoryStatAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.tvTotalItems.text = state.totalItems.toString()
                binding.tvCompletedItems.text = state.completedItems.toString()
                binding.tvPendingItems.text = state.pendingItems.toString()
                binding.tvCompletionRate.text = "${(state.completionRate * 100).roundToInt()}%"

                val progress = state.userProgress
                if (progress != null) {
                    binding.tvLevel.text = "LEVEL ${progress.level}"
                    binding.tvRank.text = progress.rank
                    binding.tvTotalXp.text = progress.totalXp.toXpString()
                    binding.tvCurrentStreak.text = "${progress.currentStreak} days"
                    binding.tvBestStreak.text = "${progress.longestStreak} days"

                    val xpPct = (XpUtils.progressPercent(progress.totalXp) * 100).roundToInt()
                    binding.progressXp.progress = xpPct
                    val currentLevelXp = XpUtils.xpForLevel(progress.level)
                    val nextLevelXp = XpUtils.xpForNextLevel(progress.level)
                    binding.tvXpCurrent.text = "${progress.totalXp - currentLevelXp} XP"
                    binding.tvXpNextLevel.text = "${nextLevelXp - progress.totalXp} XP to next level"
                }

                categoryStatAdapter.submitList(state.categories)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
