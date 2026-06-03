package com.reqsync.app.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.reqsync.app.ReqSyncApp
import com.reqsync.app.data.database.entities.*
import com.reqsync.app.data.repository.GamificationRepository
import com.reqsync.app.data.repository.NoteRepository
import com.reqsync.app.data.repository.RequirementRepository
import com.reqsync.app.utils.CategoryProgressHelper
import com.reqsync.app.utils.ReqParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ParseViewModel — handles raw text → parsed session → DB save
// ─────────────────────────────────────────────────────────────────────────────
sealed class ParseState {
    object Idle : ParseState()
    object Parsing : ParseState()
    data class Parsed(val session: ReqParser.ParsedSession) : ParseState()
    data class Saved(val sessionId: Long, val categoryCount: Int, val itemCount: Int) : ParseState()
    data class Error(val message: String) : ParseState()
}

class ParseViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as ReqSyncApp).requirementRepository

    private val _parseState = MutableStateFlow<ParseState>(ParseState.Idle)
    val parseState: StateFlow<ParseState> = _parseState

    fun parse(rawText: String) {
        if (rawText.isBlank()) {
            _parseState.value = ParseState.Error("Please paste some requirement text first.")
            return
        }
        viewModelScope.launch {
            _parseState.value = ParseState.Parsing
            try {
                val session = ReqParser.parse(rawText)
                if (session.sections.isEmpty()) {
                    _parseState.value = ParseState.Error("No requirements detected. Try a different format.")
                } else {
                    _parseState.value = ParseState.Parsed(session)
                }
            } catch (e: Exception) {
                _parseState.value = ParseState.Error("Parse error: ${e.message}")
            }
        }
    }

    fun saveSession(session: ReqParser.ParsedSession) {
        viewModelScope.launch {
            try {
                val (categories, itemsByIndex) = ReqParser.toEntities(session)
                val idMap = repo.saveParsedSession(categories, itemsByIndex)
                val totalItems = itemsByIndex.values.sumOf { it.size }
                _parseState.value = ParseState.Saved(session.sessionId, categories.size, totalItems)
            } catch (e: Exception) {
                _parseState.value = ParseState.Error("Save error: ${e.message}")
            }
        }
    }

    fun reset() { _parseState.value = ParseState.Idle }
}

// ─────────────────────────────────────────────────────────────────────────────
// ChecklistViewModel — drives the main checklist screen
// ─────────────────────────────────────────────────────────────────────────────
data class ChecklistUiState(
    val categories: List<RequirementCategory> = emptyList(),
    val itemsByCategory: Map<Long, List<RequirementItem>> = emptyMap(),
    val categoryStats: Map<Long, CategoryProgressHelper.CategoryStats> = emptyMap(),
    val searchQuery: String = "",
    val filterStatus: RequirementStatus? = null,
    val isLoading: Boolean = true,
    val xpGainEvent: Int? = null,   // one-shot XP animation trigger
    val dialogEvent: DialogEvent? = null
)

sealed class DialogEvent {
    data class Archived(val title: String) : DialogEvent()
    data class CategoryCompleted(val categoryName: String) : DialogEvent()
}

class ChecklistViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReqSyncApp
    private val reqRepo = app.requirementRepository
    private val gamRepo = app.gamificationRepository

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow<RequirementStatus?>(null)
    private val _dialogEvent = MutableStateFlow<DialogEvent?>(null)
    private val _xpEvent = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ChecklistUiState> = combine(
        reqRepo.getAllCategories(),
        reqRepo.getActiveItems(),
        _searchQuery,
        _filterStatus,
        _dialogEvent
    ) { categories, allItems, query, filter, dialog ->
        // Calculate stats based on ALL items to ensure progress is accurate even when filtered
        val stats = CategoryProgressHelper.computeStats(categories, allItems)

        val filteredItems = allItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            val matchesFilter = filter == null || item.status == filter
            matchesQuery && matchesFilter
        }
        val itemsByCategory = filteredItems.groupBy { it.categoryId }
        ChecklistUiState(
            categories = categories,
            itemsByCategory = itemsByCategory,
            categoryStats = stats,
            searchQuery = query,
            filterStatus = filter,
            isLoading = false,
            dialogEvent = dialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChecklistUiState())

    val xpEvent: StateFlow<Int?> = _xpEvent

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilter(status: RequirementStatus?) { _filterStatus.value = status }

    fun toggleItemStatus(item: RequirementItem) {
        viewModelScope.launch {
            val newStatus = if (item.status == RequirementStatus.COMPLETED)
                RequirementStatus.PENDING else RequirementStatus.COMPLETED
            reqRepo.updateStatus(item.id, newStatus)

            if (newStatus == RequirementStatus.COMPLETED) {
                gamRepo.addXp(item.xpReward)
                gamRepo.incrementCompleted()
                gamRepo.updateStreak()
                val progress = gamRepo.getProgressOnce()
                gamRepo.checkCompletionAchievements(progress?.totalCompleted ?: 0)
                _xpEvent.value = item.xpReward

                // Check if category is now completed
                val currentItems = reqRepo.getItemsByCategory(item.categoryId).first()
                val isCatComplete = currentItems.all { it.status == RequirementStatus.COMPLETED }
                if (isCatComplete) {
                    val cat = reqRepo.getCategoryById(item.categoryId)
                    _dialogEvent.value = DialogEvent.CategoryCompleted(cat?.title ?: "Category")
                }
            } else {
                // Decrease XP when unchecking an item
                gamRepo.removeXp(item.xpReward)
                gamRepo.decrementCompleted()
                _xpEvent.value = -item.xpReward
            }
        }
    }

    fun consumeXpEvent() { _xpEvent.value = null }
    fun consumeDialogEvent() { _dialogEvent.value = null }

    fun toggleCategoryExpanded(categoryId: Long, expanded: Boolean) {
        viewModelScope.launch { reqRepo.toggleCategoryExpanded(categoryId, expanded) }
    }

    fun archiveCategory(categoryId: Long) {
        viewModelScope.launch {
            val cat = reqRepo.getCategoryById(categoryId)
            reqRepo.archiveCategory(categoryId)
            _dialogEvent.value = DialogEvent.Archived(cat?.title ?: "Category")
        }
    }

    fun archiveItem(itemId: Long) {
        viewModelScope.launch {
            val item = reqRepo.getItemById(itemId)
            reqRepo.archiveItem(itemId)
            _dialogEvent.value = DialogEvent.Archived(item?.title ?: "Requirement")
        }
    }

    fun deleteItem(item: RequirementItem) {
        viewModelScope.launch { reqRepo.deleteItem(item) }
    }

    fun deleteCategory(category: RequirementCategory) {
        viewModelScope.launch { reqRepo.deleteCategory(category) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DetailsViewModel — single requirement item detail
// ─────────────────────────────────────────────────────────────────────────────
class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReqSyncApp
    private val reqRepo = app.requirementRepository
    private val noteRepo = app.noteRepository
    private val gamRepo = app.gamificationRepository

    private val _itemId = MutableStateFlow<Long>(-1L)
    private val _dialogEvent = MutableStateFlow<DialogEvent?>(null)

    val item: StateFlow<RequirementItem?> = _itemId
        .filter { it > 0 }
        .flatMapLatest { id ->
            reqRepo.getAllItems().map { items -> items.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dialogEvent: StateFlow<DialogEvent?> = _dialogEvent

    val notes: StateFlow<List<Note>> = _itemId
        .filter { it > 0 }
        .flatMapLatest { noteRepo.getNotesByItem(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setItemId(id: Long) { _itemId.value = id }
    fun consumeDialogEvent() { _dialogEvent.value = null }

    fun markComplete() {
        viewModelScope.launch {
            val current = item.value ?: return@launch
            reqRepo.updateStatus(current.id, RequirementStatus.COMPLETED)
            gamRepo.addXp(current.xpReward)
            gamRepo.incrementCompleted()
            gamRepo.updateStreak()

            // Check if category is now completed
            val currentItems = reqRepo.getItemsByCategory(current.categoryId).first()
            val isCatComplete = currentItems.all { it.status == RequirementStatus.COMPLETED }
            if (isCatComplete) {
                val cat = reqRepo.getCategoryById(current.categoryId)
                _dialogEvent.value = DialogEvent.CategoryCompleted(cat?.title ?: "Category")
            }
        }
    }

    fun updateStatus(status: RequirementStatus) {
        viewModelScope.launch {
            val current = item.value ?: return@launch
            reqRepo.updateStatus(current.id, status)
        }
    }

    fun addNote(content: String) {
        viewModelScope.launch {
            val id = _itemId.value
            if (id > 0 && content.isNotBlank()) {
                noteRepo.insertNote(Note(requirementItemId = id, content = content))
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepo.deleteNote(note) }
    }
}
