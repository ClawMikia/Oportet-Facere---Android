package com.reqsync.app.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.reqsync.app.adapters.AchievementAdapter
import com.reqsync.app.databinding.FragmentAchievementsBinding
import com.reqsync.app.viewmodels.AchievementsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AchievementsFragment : Fragment() {
    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AchievementsViewModel by viewModels()
    private lateinit var adapter: AchievementAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AchievementAdapter()
        binding.rvAchievements.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAchievements.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                adapter.submitList(state.allAchievements)
                binding.tvUnlockedCount.text = state.unlockedCount.toString()
                binding.tvTotalCount.text = " / ${state.totalCount} UNLOCKED"
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
