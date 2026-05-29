package com.reqsync.app.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.reqsync.app.adapters.ReminderAdapter
import com.reqsync.app.data.database.entities.Reminder
import com.reqsync.app.databinding.FragmentRemindersBinding
import com.reqsync.app.viewmodels.RemindersViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RemindersViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReminderAdapter { reminder -> viewModel.deleteReminder(reminder) }
        binding.rvReminders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReminders.adapter = adapter

        binding.btnAddReminder.setOnClickListener { showDateTimePicker() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeReminders.collectLatest { reminders ->
                adapter.submitList(reminders)
                binding.tvEmpty.visibility = if (reminders.isEmpty()) View.VISIBLE else View.GONE
                binding.rvReminders.visibility = if (reminders.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, min ->
                cal.set(year, month, day, hour, min, 0)
                val reminder = Reminder(
                    requirementItemId = 0L,
                    title = "Mission Alert",
                    message = "You have a pending employment requirement.",
                    scheduledAt = cal.timeInMillis
                )
                viewModel.addReminder(reminder)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
