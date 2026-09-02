package com.reqsync.app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reqsync.app.data.database.entities.*
import com.reqsync.app.databinding.*
import com.reqsync.app.utils.CategoryProgressHelper
import com.reqsync.app.utils.slideInFromRight
import com.reqsync.app.utils.toColorInt
import com.reqsync.app.utils.toFormattedDate
import com.reqsync.app.utils.toRelativeTime

/**
 * Shared list item that pairs a [RequirementCategory] with its pre-computed
 * completion [CategoryProgressHelper.CategoryStats].
 *
 * Bundling the stats into the item — instead of holding them in a separate map
 * whose setter calls [RecyclerView.Adapter.notifyDataSetChanged] — lets the
 * ListAdapter's DiffUtil detect stat changes and rebind only what changed. This
 * avoids the notifyDataSetChanged + submitList conflict that causes rebind churn
 * and duplicate-looking rows.
 */
data class CategoryStatItem(
    val category: RequirementCategory,
    val stats: CategoryProgressHelper.CategoryStats? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// CategorySummaryAdapter — dashboard category list
// ─────────────────────────────────────────────────────────────────────────────
class CategorySummaryAdapter(
    private val onClick: (RequirementCategory) -> Unit,
    private val onArchive: (RequirementCategory) -> Unit
) : ListAdapter<CategoryStatItem, CategorySummaryAdapter.VH>(
    object : DiffUtil.ItemCallback<CategoryStatItem>() {
        override fun areItemsTheSame(o: CategoryStatItem, n: CategoryStatItem) =
            o.category.id == n.category.id
        override fun areContentsTheSame(o: CategoryStatItem, n: CategoryStatItem) = o == n
        override fun getChangePayload(o: CategoryStatItem, n: CategoryStatItem): Any? =
            if (areItemsTheSame(o, n) && !areContentsTheSame(o, n)) StatPayload.REFRESH else null
    }
) {
    enum class StatPayload { REFRESH }

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), animate = true)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        if (payloads.any { it == StatPayload.REFRESH }) {
            holder.bind(getItem(position), animate = false)
        } else {
            holder.bind(getItem(position), animate = true)
        }
    }

    private fun VH.bind(item: CategoryStatItem, animate: Boolean) {
        val cat = item.category
        with(binding) {
            if (animate) itemView.slideInFromRight(absoluteAdapterPosition)
            tvCategoryTitle.text = cat.title
            val color = cat.colorTag.toColorInt()
            viewColorDot.backgroundTintList = ColorStateList.valueOf(color)
            progressCategory.progressTintList = ColorStateList.valueOf(color)
            progressBarFull.progressTintList = ColorStateList.valueOf(color)
            tvExpandIcon.text = "›"

            val stats = item.stats
            if (stats != null) {
                tvProgressText.text = stats.progressText
                tvPercent.text = "  •  ${stats.percentText}"
                progressCategory.progress = stats.percent
                progressBarFull.progress = stats.percent
            } else {
                tvProgressText.text = "0 / 0"
                tvPercent.text = "  •  0%"
                progressCategory.progress = 0
                progressBarFull.progress = 0
            }

            btnArchive.setOnClickListener { onArchive(cat) }
            root.setOnClickListener { onClick(cat) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PreviewCategoryAdapter — paste screen parsed preview
// ─────────────────────────────────────────────────────────────────────────────
class PreviewCategoryAdapter : ListAdapter<com.reqsync.app.utils.ReqParser.ParsedSection,
        PreviewCategoryAdapter.VH>(
    object : DiffUtil.ItemCallback<com.reqsync.app.utils.ReqParser.ParsedSection>() {
        override fun areItemsTheSame(o: com.reqsync.app.utils.ReqParser.ParsedSection, n: com.reqsync.app.utils.ReqParser.ParsedSection) = o.title == n.title
        override fun areContentsTheSame(o: com.reqsync.app.utils.ReqParser.ParsedSection, n: com.reqsync.app.utils.ReqParser.ParsedSection) = o == n
    }
) {
    inner class VH(val binding: ItemPreviewCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemPreviewCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val section = getItem(position)
        with(holder.binding) {
            holder.itemView.slideInFromRight(position)
            tvCategoryName.text = "▸ ${section.title}"
            tvItemsPreview.text = section.items.joinToString("\n") { "  • ${it.title}" }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AchievementAdapter
// ─────────────────────────────────────────────────────────────────────────────
class AchievementAdapter : ListAdapter<Achievement, AchievementAdapter.VH>(
    object : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(o: Achievement, n: Achievement) = o.id == n.id
        override fun areContentsTheSame(o: Achievement, n: Achievement) = o == n
    }
) {
    inner class VH(val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = getItem(position)
        with(holder.binding) {
            holder.itemView.slideInFromRight(position)
            tvAchievementTitle.text = a.title
            tvAchievementDesc.text = a.description
            tvXpReward.text = "+${a.xpReward} XP"

            if (a.isUnlocked) {
                cardAchievement.alpha = 1f
                viewBadgeBg.setBackgroundResource(com.reqsync.app.R.drawable.bg_achievement_unlocked)
                tvBadgeIcon.text = "★"
                tvBadgeIcon.setTextColor(0xFFBF00FF.toInt())
                tvLockIcon.text = "🏅"
                tvUnlockedAt.visibility = android.view.View.VISIBLE
                tvUnlockedAt.text = "Unlocked ${a.unlockedAt?.toFormattedDate() ?: ""}"
            } else {
                cardAchievement.alpha = 0.55f
                viewBadgeBg.setBackgroundResource(com.reqsync.app.R.drawable.bg_achievement_locked)
                tvBadgeIcon.text = "?"
                tvBadgeIcon.setTextColor(0xFF4A5568.toInt())
                tvLockIcon.text = "🔒"
                tvUnlockedAt.visibility = android.view.View.GONE
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NoteAdapter
// ─────────────────────────────────────────────────────────────────────────────
class NoteAdapter(
    private val onDelete: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.VH>(
    object : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(o: Note, n: Note) = o.id == n.id
        override fun areContentsTheSame(o: Note, n: Note) = o == n
    }
) {
    inner class VH(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val note = getItem(position)
        with(holder.binding) {
            holder.itemView.slideInFromRight(position)
            tvNoteContent.text = note.content
            tvNoteTime.text = note.createdAt.toRelativeTime()
            btnDeleteNote.setOnClickListener { onDelete(note) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ReminderAdapter
// ─────────────────────────────────────────────────────────────────────────────
class ReminderAdapter(
    private val onDelete: (Reminder) -> Unit
) : ListAdapter<Reminder, ReminderAdapter.VH>(
    object : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(o: Reminder, n: Reminder) = o.id == n.id
        override fun areContentsTheSame(o: Reminder, n: Reminder) = o == n
    }
) {
    inner class VH(val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val reminder = getItem(position)
        with(holder.binding) {
            holder.itemView.slideInFromRight(position)
            tvReminderTitle.text = reminder.title
            tvReminderTime.text = reminder.scheduledAt.toFormattedDate()
            btnDeleteReminder.setOnClickListener { onDelete(reminder) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryStatAdapter — statistics screen breakdown
// ─────────────────────────────────────────────────────────────────────────────
class CategoryStatAdapter : ListAdapter<CategoryStatItem, CategoryStatAdapter.VH>(
    object : DiffUtil.ItemCallback<CategoryStatItem>() {
        override fun areItemsTheSame(o: CategoryStatItem, n: CategoryStatItem) =
            o.category.id == n.category.id
        override fun areContentsTheSame(o: CategoryStatItem, n: CategoryStatItem) = o == n
        override fun getChangePayload(o: CategoryStatItem, n: CategoryStatItem): Any? =
            if (areItemsTheSame(o, n) && !areContentsTheSame(o, n)) StatPayload.REFRESH else null
    }
) {
    enum class StatPayload { REFRESH }

    inner class VH(val binding: ItemCategoryStatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), animate = true)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        if (payloads.any { it == StatPayload.REFRESH }) {
            holder.bind(getItem(position), animate = false)
        } else {
            holder.bind(getItem(position), animate = true)
        }
    }

    private fun VH.bind(item: CategoryStatItem, animate: Boolean) {
        val cat = item.category
        with(binding) {
            if (animate) itemView.slideInFromRight(absoluteAdapterPosition)
            tvCatName.text = cat.title
            val color = cat.colorTag.toColorInt()
            viewColor.backgroundTintList = ColorStateList.valueOf(color)
            progressCat.progressTintList = ColorStateList.valueOf(color)

            val stats = item.stats
            if (stats != null) {
                tvCatProgress.text = stats.percentText
                progressCat.progress = stats.percent
            } else {
                tvCatProgress.text = "0%"
                progressCat.progress = 0
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TimelineAdapter — mission timeline screen
// ─────────────────────────────────────────────────────────────────────────────
class TimelineAdapter : ListAdapter<CategoryStatItem, TimelineAdapter.VH>(
    object : DiffUtil.ItemCallback<CategoryStatItem>() {
        override fun areItemsTheSame(o: CategoryStatItem, n: CategoryStatItem) =
            o.category.id == n.category.id
        override fun areContentsTheSame(o: CategoryStatItem, n: CategoryStatItem) = o == n
        override fun getChangePayload(o: CategoryStatItem, n: CategoryStatItem): Any? =
            if (areItemsTheSame(o, n) && !areContentsTheSame(o, n)) StatPayload.REFRESH else null
    }
) {
    enum class StatPayload { REFRESH }

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), animate = true)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        if (payloads.any { it == StatPayload.REFRESH }) {
            holder.bind(getItem(position), animate = false)
        } else {
            holder.bind(getItem(position), animate = true)
        }
    }

    private fun VH.bind(item: CategoryStatItem, animate: Boolean) {
        val cat = item.category
        with(binding) {
            if (animate) itemView.slideInFromRight(absoluteAdapterPosition)
            tvCategoryTitle.text = cat.title
            val color = cat.colorTag.toColorInt()
            viewColorDot.backgroundTintList = ColorStateList.valueOf(color)
            progressCategory.progressTintList = ColorStateList.valueOf(color)
            progressBarFull.progressTintList = ColorStateList.valueOf(color)
            tvExpandIcon.text = if (absoluteAdapterPosition == 0) "◉" else "○"
            btnArchive.visibility = android.view.View.GONE

            val stats = item.stats
            if (stats != null) {
                tvProgressText.text = stats.progressText
                tvPercent.text = "  •  ${stats.percentText}"
                progressCategory.progress = stats.percent
                progressBarFull.progress = stats.percent
            } else {
                tvProgressText.text = "0 / 0"
                tvPercent.text = "  •  0%"
                progressCategory.progress = 0
                progressBarFull.progress = 0
            }
        }
    }
}
