package com.reqsync.app.adapters

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reqsync.app.R
import com.reqsync.app.data.database.entities.RequirementCategory
import com.reqsync.app.data.database.entities.RequirementItem
import com.reqsync.app.data.database.entities.RequirementStatus
import com.reqsync.app.databinding.ItemCategoryBinding
import com.reqsync.app.databinding.ItemRequirementBinding
import com.reqsync.app.utils.toColorInt

/**
 * Flat list adapter that renders two view types:
 *  1. CategoryHeader — expandable group header
 *  2. RequirementRow — individual checklist item
 */
class ChecklistAdapter(
    private val onItemChecked: (RequirementItem) -> Unit,
    private val onItemClicked: (RequirementItem) -> Unit,
    private val onCategoryToggled: (RequirementCategory, Boolean) -> Unit
) : ListAdapter<ChecklistAdapter.ListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    /** Updated externally whenever item counts change. */
    var statsMap: Map<Long, com.reqsync.app.utils.CategoryProgressHelper.CategoryStats> = emptyMap()
        set(value) { field = value; notifyDataSetChanged() }

    sealed class ListItem {
        data class CategoryHeader(val category: RequirementCategory) : ListItem()
        data class RequirementRow(val item: RequirementItem) : ListItem()
    }

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ListItem.CategoryHeader -> TYPE_CATEGORY
        is ListItem.RequirementRow -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY -> CategoryViewHolder(
                ItemCategoryBinding.inflate(inflater, parent, false)
            )
            else -> ItemViewHolder(
                ItemRequirementBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.CategoryHeader -> (holder as CategoryViewHolder).bind(item.category)
            is ListItem.RequirementRow -> (holder as ItemViewHolder).bind(item.item)
        }
    }

    // ── Category ViewHolder ───────────────────────────────────────────────────
    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: RequirementCategory) {
            binding.tvCategoryTitle.text = category.title
            val color = category.colorTag.toColorInt()
            binding.viewColorDot.backgroundTintList = ColorStateList.valueOf(color)
            binding.progressCategory.progressTintList = ColorStateList.valueOf(color)
            binding.progressBarFull.progressTintList = ColorStateList.valueOf(color)
            binding.tvExpandIcon.text = if (category.isExpanded) "▼" else "▶"

            val stats = statsMap[category.id]
            if (stats != null) {
                binding.tvProgressText.text = stats.progressText
                binding.tvPercent.text = "  •  ${stats.percentText}"
                binding.progressCategory.progress = stats.percent
                binding.progressBarFull.progress = stats.percent
            } else {
                binding.tvProgressText.text = "0 / 0"
                binding.tvPercent.text = "  •  0%"
                binding.progressCategory.progress = 0
                binding.progressBarFull.progress = 0
            }

            binding.layoutHeader.setOnClickListener {
                onCategoryToggled(category, !category.isExpanded)
            }
        }
    }

    // ── Item ViewHolder ───────────────────────────────────────────────────────
    inner class ItemViewHolder(private val binding: ItemRequirementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RequirementItem) {
            binding.tvTitle.text = item.title
            binding.tvXpReward.text = "+${item.xpReward} XP"
            binding.tvOptional.visibility = if (item.isOptional) View.VISIBLE else View.GONE
            binding.tvNoteIndicator.visibility = if (item.hasNote) View.VISIBLE else View.GONE

            val isCompleted = item.status == RequirementStatus.COMPLETED

            // Checkbox state (no listener during bind to avoid loops)
            binding.cbComplete.setOnCheckedChangeListener(null)
            binding.cbComplete.isChecked = isCompleted

            // Strike-through title when completed
            if (isCompleted) {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTitle.alpha = 0.5f
                binding.viewCompletedGlow.visibility = View.VISIBLE
            } else {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTitle.alpha = 1f
                binding.viewCompletedGlow.visibility = View.GONE
            }

            // Status badge
            when (item.status) {
                RequirementStatus.COMPLETED -> {
                    binding.tvStatusBadge.text = "COMPLETED"
                    binding.tvStatusBadge.setTextColor(0xFF00FF9F.toInt())
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed)
                }
                RequirementStatus.OVERDUE -> {
                    binding.tvStatusBadge.text = "OVERDUE"
                    binding.tvStatusBadge.setTextColor(0xFFFF3D3D.toInt())
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_overdue)
                }
                RequirementStatus.IN_PROGRESS -> {
                    binding.tvStatusBadge.text = "IN PROGRESS"
                    binding.tvStatusBadge.setTextColor(0xFFFF9F00.toInt())
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                }
                else -> {
                    binding.tvStatusBadge.text = "PENDING"
                    binding.tvStatusBadge.setTextColor(0xFFFFD700.toInt())
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                }
            }

            // Click listeners
            binding.cbComplete.setOnCheckedChangeListener { _, _ -> onItemChecked(item) }
            binding.root.setOnClickListener {
                val anim = AnimationUtils.loadAnimation(it.context, R.anim.item_slide_in)
                it.startAnimation(anim)
                onItemClicked(item)
            }
        }
    }

    // ── DiffUtil ──────────────────────────────────────────────────────────────
    class DiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean = when {
            oldItem is ListItem.CategoryHeader && newItem is ListItem.CategoryHeader ->
                oldItem.category.id == newItem.category.id
            oldItem is ListItem.RequirementRow && newItem is ListItem.RequirementRow ->
                oldItem.item.id == newItem.item.id
            else -> false
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean =
            oldItem == newItem
    }
}
