package com.MatthewBaerg.multitasked.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.repository.BoardRepository
import com.MatthewBaerg.multitasked.ui.CelebrationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val repository: BoardRepository,
    private val celebrationSettings: CelebrationSettings,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val boardId: String = checkNotNull(savedStateHandle["boardId"])

    private val _sortOption = MutableStateFlow(TaskSortOption.RECENT)
    private val _showCelebration = MutableStateFlow(false)
    private val _transientErrorMessage = MutableStateFlow<String?>(null)
    private val _retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<BoardDetailUiState> = _retryTrigger.flatMapLatest {
        combine(
            repository.getBoard(boardId),
            repository.getTasks(boardId),
            repository.getBoardMembers(boardId),
            _sortOption,
            _showCelebration
        ) { board, tasks, members, sortOption, showCelebration ->
            BoardDetailUiState(
                board = board,
                tasks = TaskSortPolicy.sort(tasks, sortOption),
                boardMembers = members,
                sortOption = sortOption,
                showCelebration = showCelebration
            )
        }.catch {
            emit(BoardDetailUiState(errorMessage = "Failed to load board details"))
        }
    }.combine(_transientErrorMessage) { state, transientError ->
        state.copy(transientErrorMessage = transientError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardDetailUiState(isLoading = true)
    )

    fun onAction(action: BoardDetailAction) {
        when (action) {
            is BoardDetailAction.AddTask -> addTask(action.title)
            is BoardDetailAction.ToggleTask -> toggleTask(action.task)
            is BoardDetailAction.UpdateTaskPrice -> updateTaskPrice(action)
            is BoardDetailAction.UpdateTaskDetails -> updateTaskDetails(action)
            is BoardDetailAction.AssignTask -> assignTask(action)
            is BoardDetailAction.DeleteTask -> deleteTask(action.task)
            is BoardDetailAction.SortChanged -> _sortOption.value = action.option
            BoardDetailAction.ErrorShown -> _transientErrorMessage.value = null
            BoardDetailAction.CelebrationShown -> _showCelebration.value = false
            BoardDetailAction.Retry -> _retryTrigger.update { it + 1 }
        }
    }

    private fun addTask(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            _transientErrorMessage.value = "Task title cannot be empty"
            return
        }

        launchRepositoryAction("addTask", "Failed to add task") {
            repository.addTask(boardId, trimmedTitle)
        }
    }

    private fun toggleTask(task: TaskItem) {
        launchRepositoryAction("toggleTask", "Failed to update task") {
            val shouldCelebrate = shouldCelebrateAfterCompleting(task)
            repository.toggleTask(boardId, task.id, !task.isDone)

            // Show feedback only after the write succeeds.
            if (shouldCelebrate) {
                _showCelebration.value = true
            }
        }
    }

    private suspend fun shouldCelebrateAfterCompleting(task: TaskItem): Boolean {
        if (task.isDone || !celebrationSettings.showCelebration.first()) {
            return false
        }

        val currentState = uiState.value
        return currentState.totalTasks > 0 &&
            currentState.completedTasks + 1 == currentState.totalTasks
    }

    private fun updateTaskPrice(action: BoardDetailAction.UpdateTaskPrice) {
        if (!action.price.isFinite() || action.price < 0.0) {
            _transientErrorMessage.value = "Price must be zero or greater"
            return
        }

        launchRepositoryAction("updateTaskPrice", "Failed to update price") {
            repository.updateTaskPrice(boardId, action.task.id, action.price)
        }
    }

    private fun updateTaskDetails(action: BoardDetailAction.UpdateTaskDetails) {
        launchRepositoryAction("updateTaskDetails", "Failed to update details") {
            repository.updateTaskDetails(
                boardId = boardId,
                taskId = action.task.id,
                notes = action.notes.trim(),
                dueDate = action.dueDate,
                priority = action.priority
            )
        }
    }

    private fun assignTask(action: BoardDetailAction.AssignTask) {
        if (action.userId.isBlank()) {
            _transientErrorMessage.value = "Select a valid board member"
            return
        }

        launchRepositoryAction("assignTask", "Failed to assign task") {
            repository.assignTask(boardId, action.task.id, action.userId)
        }
    }

    private fun deleteTask(task: TaskItem) {
        launchRepositoryAction("deleteTask", "Failed to delete task") {
            repository.deleteTask(boardId, task.id)
        }
    }

    private fun launchRepositoryAction(
        actionName: String,
        userMessage: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _transientErrorMessage.value = userMessage
                logErrorSafely(actionName, exception.javaClass.simpleName)
            }
        }
    }

    private suspend fun logErrorSafely(actionName: String, errorType: String) {
        try {
            repository.logError(actionName, errorType, boardId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // The original operation's user-facing error remains available.
        }
    }
}
