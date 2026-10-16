package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User

enum class TaskSortOption {
    RECENT,
    ALPHA_ASC,
    ALPHA_DESC,
    COMPLETED_FIRST,
    INCOMPLETE_FIRST,
    DUE_DATE,
    URGENCY
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

sealed interface BoardDetailAction {
    data class AddTask(val title: String) : BoardDetailAction
    data class ToggleTask(val task: TaskItem) : BoardDetailAction
    data class UpdateTaskPrice(val task: TaskItem, val price: Double) : BoardDetailAction
    data class UpdateTaskDetails(
        val task: TaskItem,
        val notes: String,
        val dueDate: Long?,
        val priority: Priority
    ) : BoardDetailAction

    data class AssignTask(val task: TaskItem, val userId: String) : BoardDetailAction
    data class DeleteTask(val task: TaskItem) : BoardDetailAction
    data class SortChanged(val option: TaskSortOption) : BoardDetailAction
    data object ErrorShown : BoardDetailAction
    data object CelebrationShown : BoardDetailAction
    data object Retry : BoardDetailAction
}
