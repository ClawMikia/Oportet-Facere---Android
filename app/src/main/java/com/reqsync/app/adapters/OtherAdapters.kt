package com.reqsync.app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reqsync.app.data.database.entities.*
import com.reqsync.app.databinding.*
import com.reqsync.app.utils.toColorInt
import com.reqsync.app.utils.toFormattedDate
import com.reqsync.app.utils.toRelativeTime

// ─────────────────────────────────────────────────────────────────────────────
// CategorySummaryAdapter — dashboard category list
// ─────────────────────────────────────────────────────────────────────────────
class CategorySummaryAdapter(
    private val onClick: (RequirementCategory) -> Unit,
    private val onArchive: (RequirementCategory) -> Unit
) : ListAdapter<RequirementCategory, CategorySummaryAdapter.VH>(
    object : DiffUtil.ItemCallback<RequirementCategory>() {
        override fun areItemsTheSame(o: RequirementCategory, n: RequirementCategory) = o.id == n.id
        override fun areContentsTheSame(o: RequirementCategory, n: RequirementCategory) = o == n
    }
) {
    var statsMap: Map<Long, com.reqsync.app.utils.CategoryProgressHelper.CategoryStats> = emptyMap()
        set(value) { field = value; notifyDataSetChanged() }

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = getItem(position)
        with(holder.binding) {
            tvCategoryTitle.text = cat.title
            val color = cat.colorTag.toColorInt()
            viewColorDot.backgroundTintList = ColorStateList.valueOf(color)
            progressCategory.progressTintList = ColorStateList.valueOf(color)
            progressBarFull.progressTintList = ColorStateList.valueOf(color)
            tvExpandIcon.text = "›"

            val stats = statsMap[cat.id]
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
            tvReminderTitle.text = reminder.title
            tvReminderTime.text = reminder.scheduledAt.toFormattedDate()
            btnDeleteReminder.setOnClickListener { onDelete(reminder) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryStatAdapter — statistics screen breakdown
// ─────────────────────────────────────────────────────────────────────────────
class CategoryStatAdapter : ListAdapter<RequirementCategory, CategoryStatAdapter.VH>(
    object : DiffUtil.ItemCallback<RequirementCategory>() {
        override fun areItemsTheSame(o: RequirementCategory, n: RequirementCategory) = o.id == n.id
        override fun areContentsTheSame(o: RequirementCategory, n: RequirementCategory) = o == n
    }
) {
    var statsMap: Map<Long, com.reqsync.app.utils.CategoryProgressHelper.CategoryStats> = emptyMap()
        set(value) { field = value; notifyDataSetChanged() }

    inner class VH(val binding: ItemCategoryStatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = getItem(position)
        with(holder.binding) {
            tvCatName.text = cat.title
            val color = cat.colorTag.toColorInt()
            viewColor.backgroundTintList = ColorStateList.valueOf(color)
            progressCat.progressTintList = ColorStateList.valueOf(color)

            val stats = statsMap[cat.id]
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
class TimelineAdapter : ListAdapter<RequirementCategory, TimelineAdapter.VH>(
    object : DiffUtil.ItemCallback<RequirementCategory>() {
        override fun areItemsTheSame(o: RequirementCategory, n: RequirementCategory) = o.id == n.id
        override fun areContentsTheSame(o: RequirementCategory, n: RequirementCategory) = o == n
    }
) {
    var statsMap: Map<Long, com.reqsync.app.utils.CategoryProgressHelper.CategoryStats> = emptyMap()
        set(value) { field = value; notifyDataSetChanged() }

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = getItem(position)
        with(holder.binding) {
            tvCategoryTitle.text = cat.title
            val color = cat.colorTag.toColorInt()
            viewColorDot.backgroundTintList = ColorStateList.valueOf(color)
            progressCategory.progressTintList = ColorStateList.valueOf(color)
            progressBarFull.progressTintList = ColorStateList.valueOf(color)
            tvExpandIcon.text = if (position == 0) "◉" else "○"
            btnArchive.visibility = android.view.View.GONE

            val stats = statsMap[cat.id]
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
