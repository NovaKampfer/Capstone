package com.MatthewBaerg.multitasked.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User
import com.MatthewBaerg.multitasked.data.repository.BoardRepository
import com.MatthewBaerg.multitasked.ui.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskSortOption {
    RECENT,
    ALPHA_ASC,
    ALPHA_DESC,
    COMPLETED_FIRST,
    INCOMPLETE_FIRST
}

data class BoardDetailUiState(
    val isLoading: Boolean = false,
    val board: Board? = null,
    val tasks: List<TaskItem> = emptyList(),
    val boardMembers: List<User> = emptyList(),
    val errorMessage: String? = null,
    val transientErrorMessage: String? = null,
    val sortOption: TaskSortOption = TaskSortOption.RECENT,
    val showCelebration: Boolean = false
) {
    val totalTasks: Int get() = tasks.size
    val completedTasks: Int get() = tasks.count { it.isDone }
    val progress: Float
        get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()
    val totalCost: Double
        get() = tasks.sumOf { it.price }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val repo: BoardRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val boardId: String = checkNotNull(savedStateHandle["boardId"])

    private val _sortOption = MutableStateFlow(TaskSortOption.RECENT)
    private val _showCelebration = MutableStateFlow(false)
    private val _transientErrorMessage = MutableStateFlow<String?>(null)
    private val _retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<BoardDetailUiState> = _retryTrigger.flatMapLatest {
        combine(
            repo.getBoard(boardId),
            repo.getTasks(boardId),
            repo.getBoardMembers(boardId),
            _sortOption,
            _showCelebration
        ) { board, tasks, members, sortOption, showCelebration ->
            BoardDetailUiState(
                board = board,
                tasks = applySort(tasks, sortOption),
                boardMembers = members,
                sortOption = sortOption,
                showCelebration = showCelebration
            )
        }.catch { e ->
            emit(BoardDetailUiState(errorMessage = e.localizedMessage ?: "Failed to load board details"))
        }
    }.combine(_transientErrorMessage) { state, transientError ->
        state.copy(transientErrorMessage = transientError)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoardDetailUiState(isLoading = true))

    fun addTask(title: String) {
        viewModelScope.launch {
            try {
                repo.addTask(boardId, title)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to add task"
                repo.logError("addTask", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun toggleTaskDone(task: TaskItem) {
        viewModelScope.launch {
            try {
                val showCelebration = settingsRepository.showCelebration.first()
                if (!task.isDone && showCelebration) {
                    val completed = uiState.value.completedTasks + 1
                    val total = uiState.value.totalTasks
                    if (total > 0 && completed == total) {
                        _showCelebration.value = true
                    }
                }
                repo.toggleTask(boardId, task.id, !task.isDone)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to update task"
                repo.logError("toggleTaskDone", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun updateTaskPrice(task: TaskItem, price: Double) {
        viewModelScope.launch {
            try {
                repo.updateTaskPrice(boardId, task.id, price)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to update price"
                repo.logError("updateTaskPrice", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun updateTaskDetails(task: TaskItem, notes: String, dueDate: Long?, priority: Priority) {
        viewModelScope.launch {
            try {
                repo.updateTaskDetails(boardId, task.id, notes, dueDate, priority)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to update details"
                repo.logError("updateTaskDetails", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun assignTask(task: TaskItem, userId: String) {
        viewModelScope.launch {
            try {
                repo.assignTask(boardId, task.id, userId)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to assign task"
                repo.logError("assignTask", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            try {
                repo.deleteTask(boardId, task.id)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to delete task"
                repo.logError("deleteTask", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }
    
    fun errorShown() {
        _transientErrorMessage.value = null
    }

    fun setSort(option: TaskSortOption) {
        _sortOption.value = option
    }

    fun onCelebrationShown() {
        _showCelebration.value = false
    }

    fun onRetry() {
        _retryTrigger.value++
    }

    private fun applySort(
        tasks: List<TaskItem>,
        option: TaskSortOption
    ): List<TaskItem> =
        when (option) {
            TaskSortOption.RECENT ->
                tasks.sortedByDescending { it.id }

            TaskSortOption.ALPHA_ASC ->
                tasks.sortedBy { it.title.lowercase() }

            TaskSortOption.ALPHA_DESC ->
                tasks.sortedByDescending { it.title.lowercase() }

            TaskSortOption.COMPLETED_FIRST ->
                tasks.sortedWith(
                    compareByDescending<TaskItem> { it.isDone }
                        .thenBy { it.title.lowercase() }
                )

            TaskSortOption.INCOMPLETE_FIRST ->
                tasks.sortedWith(
                    compareBy<TaskItem> { it.isDone }
                        .thenBy { it.title.lowercase() }
                )
        }
}
