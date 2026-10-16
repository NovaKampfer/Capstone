package com.MatthewBaerg.multitasked.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.data.repository.BoardRepository
import com.MatthewBaerg.multitasked.ui.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BoardSortOption {
    NAME_ASC,
    NAME_DESC,
    TASK_COUNT_ASC,
    TASK_COUNT_DESC
}

data class BoardsUiState(
    val isLoading: Boolean = false,
    val allBoards: List<Board> = emptyList(),
    val searchQuery: String = "",
    val sortOption: BoardSortOption = BoardSortOption.NAME_ASC,
    val showCelebration: Boolean = true,
    val errorMessage: String? = null,
    val transientErrorMessage: String? = null
) {
    val filteredBoards: List<Board>
        get() {
            val filtered = if (searchQuery.isBlank()) {
                allBoards
            } else {
                allBoards.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            return when (sortOption) {
                BoardSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                BoardSortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
                BoardSortOption.TASK_COUNT_ASC -> filtered.sortedBy { it.taskCount }
                BoardSortOption.TASK_COUNT_DESC -> filtered.sortedByDescending { it.taskCount }
            }
        }
}

@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(BoardSortOption.NAME_ASC)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _transientErrorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BoardsUiState> = combine(
        boardRepository.getBoards(),
        settingsRepository.showCelebration,
        _searchQuery,
        _sortOption,
        _errorMessage
    ) { boards, showCelebration, query, sort, error ->
        BoardsUiState(
            allBoards = boards,
            showCelebration = showCelebration,
            searchQuery = query,
            sortOption = sort,
            errorMessage = error
        )
    }.combine(_transientErrorMessage) { state, transientError ->
        state.copy(transientErrorMessage = transientError)
    }.catch { e ->
        emit(BoardsUiState(errorMessage = e.localizedMessage ?: "Failed to load boards"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoardsUiState(isLoading = true))

    fun createBoard(name: String, description: String, type: BoardType) {
        viewModelScope.launch {
            try {
                boardRepository.createBoard(name, description, type)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to create board"
                boardRepository.logError("createBoard", errorMessage, null)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun updateBoard(boardId: String, name: String, description: String) {
        viewModelScope.launch {
            try {
                boardRepository.updateBoard(boardId, name, description)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to update board"
                boardRepository.logError("updateBoard", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun joinBoard(boardId: String) {
        viewModelScope.launch {
            try {
                boardRepository.joinBoard(boardId)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to join board"
                boardRepository.logError("joinBoard", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            try {
                boardRepository.deleteBoard(boardId)
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Failed to delete board"
                boardRepository.logError("deleteBoard", errorMessage, boardId)
                _transientErrorMessage.value = errorMessage
            }
        }
    }

    fun errorShown() {
        _transientErrorMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }



    fun setSortOption(option: BoardSortOption) {
        _sortOption.value = option
    }

    fun setShowCelebration(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowCelebration(show)
        }
    }


}
