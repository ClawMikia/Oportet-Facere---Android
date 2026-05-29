package com.reqsync.app.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.reqsync.app.ReqSyncApp
import com.reqsync.app.data.database.entities.RequirementCategory
import com.reqsync.app.data.database.entities.RequirementItem
import com.reqsync.app.data.database.entities.UserProgress
import com.reqsync.app.data.repository.GamificationRepository
import com.reqsync.app.data.repository.RequirementRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val pendingItems: Int = 0,
    val completionPercent: Float = 0f,
    val categories: List<RequirementCategory> = emptyList(),
    val recentItems: List<RequirementItem> = emptyList(),
    val userProgress: UserProgress? = null,
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReqSyncApp
    private val reqRepo: RequirementRepository = app.requirementRepository
    private val gamRepo: GamificationRepository = app.gamificationRepository

    val uiState: StateFlow<DashboardUiState> = combine(
        reqRepo.getTotalCount(),
        reqRepo.getCompletedCount(),
        reqRepo.getPendingCount(),
        reqRepo.getAllCategories(),
        gamRepo.getProgress()
    ) { total, completed, pending, categories, progress ->
        val percent = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        DashboardUiState(
            totalItems = total,
            completedItems = completed,
            pendingItems = pending,
            completionPercent = percent,
            categories = categories,
            userProgress = progress,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch {
            gamRepo.ensureProgressExists()
        }
    }
}
