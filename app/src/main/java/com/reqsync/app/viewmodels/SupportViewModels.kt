package com.reqsync.app.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.reqsync.app.ReqSyncApp
import com.reqsync.app.data.database.entities.*
import com.reqsync.app.data.repository.GamificationRepository
import com.reqsync.app.data.repository.ReminderRepository
import com.reqsync.app.data.repository.RequirementRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// AchievementsViewModel
// ─────────────────────────────────────────────────────────────────────────────
data class AchievementsUiState(
    val allAchievements: List<Achievement> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val userProgress: UserProgress? = null
)

class AchievementsViewModel(application: Application) : AndroidViewModel(application) {
    private val gamRepo = (application as ReqSyncApp).gamificationRepository

    val uiState: StateFlow<AchievementsUiState> = combine(
        gamRepo.getAllAchievements(),
        gamRepo.getUnlockedCount(),
        gamRepo.getProgress()
    ) { achievements, unlocked, progress ->
        AchievementsUiState(
            allAchievements = achievements,
            unlockedCount = unlocked,
            totalCount = achievements.size,
            userProgress = progress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AchievementsUiState())
}

// ─────────────────────────────────────────────────────────────────────────────
// StatisticsViewModel
// ─────────────────────────────────────────────────────────────────────────────
data class StatisticsUiState(
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val pendingItems: Int = 0,
    val categories: List<RequirementCategory> = emptyList(),
    val allItems: List<RequirementItem> = emptyList(),
    val userProgress: UserProgress? = null,
    val completionRate: Float = 0f
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ReqSyncApp
    private val reqRepo = app.requirementRepository
    private val gamRepo = app.gamificationRepository

    val uiState: StateFlow<StatisticsUiState> = combine(
        reqRepo.getTotalCount(),
        reqRepo.getCompletedCount(),
        reqRepo.getAllCategories(),
        reqRepo.getAllItems(),
        gamRepo.getProgress()
    ) { total, completed, categories, items, progress ->
        StatisticsUiState(
            totalItems = total,
            completedItems = completed,
            pendingItems = total - completed,
            categories = categories,
            allItems = items,
            userProgress = progress,
            completionRate = if (total > 0) completed.toFloat() / total else 0f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())
}

// ─────────────────────────────────────────────────────────────────────────────
// RemindersViewModel
// ─────────────────────────────────────────────────────────────────────────────
class RemindersViewModel(application: Application) : AndroidViewModel(application) {
    private val reminderRepo = (application as ReqSyncApp).reminderRepository
    private val reqRepo      = (application as ReqSyncApp).requirementRepository

    val activeReminders: StateFlow<List<Reminder>> = reminderRepo.getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<RequirementItem>> = reqRepo.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            val id = reminderRepo.insertReminder(reminder)
            // Schedule the WorkManager job
            com.reqsync.app.utils.ReminderScheduler.schedule(
                context       = getApplication(),
                reminderId    = id,
                title         = reminder.title,
                message       = reminder.message,
                scheduledAtMillis = reminder.scheduledAt
            )
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            com.reqsync.app.utils.ReminderScheduler.cancel(getApplication(), reminder.id)
            reminderRepo.deleteReminder(reminder)
        }
    }

    fun deactivateReminder(id: Long) {
        viewModelScope.launch {
            com.reqsync.app.utils.ReminderScheduler.cancel(getApplication(), id)
            reminderRepo.deactivateReminder(id)
        }
    }
}
