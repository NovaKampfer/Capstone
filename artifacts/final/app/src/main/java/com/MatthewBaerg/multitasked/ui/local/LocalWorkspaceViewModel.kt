package com.MatthewBaerg.multitasked.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MatthewBaerg.multitasked.data.local.*
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.ui.tasks.TaskSortOption
import com.MatthewBaerg.multitasked.ui.tasks.TaskSortingService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LocalSelection(val id: String? = null, val board: BoardEntity? = null,
                          val tasks: List<TaskItem> = emptyList(),
                          val option: TaskSortOption = TaskSortOption.RECENT)
data class LocalUiState(val boards: List<BoardSummary> = emptyList(),
                        val selection: LocalSelection = LocalSelection(),
                        val loading: Boolean = true, val busy: Boolean = false,
                        val error: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalWorkspaceViewModel @Inject constructor(
    private val repository: LocalBoardRepository,
    private val sorting: TaskSortingService
) : ViewModel() {
    private val selected = MutableStateFlow<String?>(null)
    private val option = MutableStateFlow(TaskSortOption.RECENT)
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LocalUiState> = retry.flatMapLatest {
        val detail = selected.flatMapLatest { id ->
            if (id == null) flowOf(LocalSelection()) else combine(
                repository.observeBoard(id), sorting.observe(repository.observeTasks(id), option)
            ) { board, result -> LocalSelection(id, board, result.tasks, result.option) }
        }
        combine(repository.observeBoards(), detail) { boards, selection ->
            LocalUiState(boards, selection, loading = false)
        }.catch { emit(LocalUiState(loading = false, error = "Unable to read local data. Try again.")) }
    }.combine(busy) { state, saving -> state.copy(busy = saving) }
        .combine(error) { state, message -> state.copy(error = message ?: state.error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalUiState())

    fun selectBoard(id: String?) { selected.value = id; error.value = null }
    fun sortBy(value: TaskSortOption) { option.value = value }
    fun retryRead() { error.value = null; retry.value += 1 }
    fun createBoard(name: String, first: String, saved: () -> Unit) = write(saved) {
        repository.createBoard(name, first)
    }
    fun renameBoard(id: String, name: String, saved: () -> Unit) = write(saved) {
        repository.renameBoard(id, name)
    }
    fun deleteBoard(id: String, saved: () -> Unit) = write(saved) {
        repository.deleteBoard(id)
        if (selected.value == id) selected.value = null
    }
    fun saveTask(boardId: String, id: String?, title: String, notes: String,
                 date: String, priority: Priority, saved: () -> Unit) = write(saved) {
        repository.saveTask(boardId, id, title, notes, LocalDateInput.parse(date), priority)
    }
    fun complete(task: TaskItem, completed: Boolean) = write {
        repository.setCompleted(task.boardId, task.id, completed)
    }
    fun deleteTask(task: TaskItem, saved: () -> Unit) = write(saved) {
        repository.deleteTask(task.boardId, task.id)
    }

    private fun write(saved: () -> Unit = {}, action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try {
                action()
                saved()
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (invalid: IllegalArgumentException) {
                error.value = invalid.message ?: "Check the entered values"
            } catch (_: Exception) {
                error.value = "Unable to save local data. Your entry remains available; try again."
            } finally {
                busy.value = false
            }
        }
    }
}
