package com.reqsync.app.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.reqsync.app.ReqSyncApp
import com.reqsync.app.data.database.entities.*
import com.reqsync.app.data.repository.GamificationRepository
import com.reqsync.app.data.repository.RequirementRepository
import com.reqsync.app.utils.CategoryProgressHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val pendingItems: Int = 0,
    val completionPercent: Float = 0f,
    val categories: List<RequirementCategory> = emptyList(),
    val categoryStats: Map<Long, CategoryProgressHelper.CategoryStats> = emptyMap(),
    val userProgress: UserProgress? = null,
    val isLoading: Boolean = true,
    val dialogEvent: DialogEvent? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReqSyncApp
    private val reqRepo: RequirementRepository = app.requirementRepository
    private val gamRepo: GamificationRepository = app.gamificationRepository

    private val _dialogEvent = MutableStateFlow<DialogEvent?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        reqRepo.getAllCategories(),
        reqRepo.getActiveItems(),
        gamRepo.getProgress(),
        _dialogEvent
    ) { categories, allItems, progress, dialog ->
        val total = allItems.size
        val completed = allItems.count { it.status == RequirementStatus.COMPLETED }
        val pending = total - completed
        val percent = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        val stats = CategoryProgressHelper.computeStats(categories, allItems)

        DashboardUiState(
            totalItems = total,
            completedItems = completed,
            pendingItems = pending,
            completionPercent = percent,
            categories = categories,
            categoryStats = stats,
            userProgress = progress,
            isLoading = false,
            dialogEvent = dialog
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

    fun archiveCategory(categoryId: Long) {
        viewModelScope.launch {
            val cat = reqRepo.getCategoryById(categoryId)
            reqRepo.archiveCategory(categoryId)
            _dialogEvent.value = DialogEvent.Archived(cat?.title ?: "Category")
        }
    }

    fun consumeDialogEvent() { _dialogEvent.value = null }
}
