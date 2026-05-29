package com.reqsync.app.ui.paste

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.reqsync.app.R
import com.reqsync.app.adapters.PreviewCategoryAdapter
import com.reqsync.app.databinding.FragmentPasteBinding
import com.reqsync.app.viewmodels.ParseState
import com.reqsync.app.viewmodels.ParseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PasteFragment : Fragment() {

    private var _binding: FragmentPasteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParseViewModel by viewModels()
    private lateinit var previewAdapter: PreviewCategoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeState()
        setupTextWatcher()
    }

    private fun setupRecyclerView() {
        previewAdapter = PreviewCategoryAdapter()
        binding.rvPreview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = previewAdapter
        }
    }

    private fun setupTextWatcher() {
        binding.etRawInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                binding.tvCharCount.text = "$len chars"
                // Reset parsed state when user edits
                if (viewModel.parseState.value !is ParseState.Idle) {
                    viewModel.reset()
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnClear.setOnClickListener {
            binding.etRawInput.setText("")
            viewModel.reset()
        }

        binding.btnParse.setOnClickListener {
            val text = binding.etRawInput.text.toString()
            viewModel.parse(text)
        }

        binding.btnDeploy.setOnClickListener {
            val state = viewModel.parseState.value
            if (state is ParseState.Parsed) {
                viewModel.saveSession(state.session)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.parseState.collectLatest { state ->
                when (state) {
                    is ParseState.Idle -> {
                        binding.layoutLoading.visibility = View.GONE
                        binding.layoutResults.visibility = View.GONE
                        binding.tvError.visibility = View.GONE
                    }

                    is ParseState.Parsing -> {
                        binding.layoutLoading.visibility = View.VISIBLE
                        binding.layoutResults.visibility = View.GONE
                        binding.tvError.visibility = View.GONE
                        binding.btnParse.isEnabled = false
                    }

                    is ParseState.Parsed -> {
                        binding.btnParse.isEnabled = true
                        binding.layoutLoading.visibility = View.GONE
                        binding.tvError.visibility = View.GONE

                        val totalItems = state.session.sections.sumOf { it.items.size }
                        binding.tvParseSummary.text =
                            "${state.session.sections.size} categories • $totalItems items"
                        previewAdapter.submitList(state.session.sections)

                        binding.layoutResults.visibility = View.VISIBLE
                        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_scale_in)
                        binding.layoutResults.startAnimation(anim)
                    }

                    is ParseState.Saved -> {
                        binding.btnParse.isEnabled = true
                        binding.layoutLoading.visibility = View.GONE
                        // Navigate to checklist
                        findNavController().navigate(R.id.action_paste_to_checklist)
                    }

                    is ParseState.Error -> {
                        binding.btnParse.isEnabled = true
                        binding.layoutLoading.visibility = View.GONE
                        binding.layoutResults.visibility = View.GONE
                        binding.tvError.text = state.message
                        binding.tvError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
