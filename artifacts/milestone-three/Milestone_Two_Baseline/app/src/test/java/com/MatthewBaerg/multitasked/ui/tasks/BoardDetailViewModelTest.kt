package com.MatthewBaerg.multitasked.ui.tasks

import androidx.lifecycle.SavedStateHandle
import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User
import com.MatthewBaerg.multitasked.data.repository.BoardRepository
import com.MatthewBaerg.multitasked.ui.CelebrationSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun addTask_trimsTitleBeforeCallingRepository() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = createViewModel(repository)
        collectState(viewModel)

        viewModel.onAction(BoardDetailAction.AddTask("  Buy milk  "))
        advanceUntilIdle()

        assertEquals("Buy milk", repository.addedTitle)
    }

    @Test
    fun negativePrice_isRejectedBeforeCallingRepository() = runTest {
        val repository = FakeBoardRepository()
        val viewModel = createViewModel(repository)
        collectState(viewModel)

        viewModel.onAction(
            BoardDetailAction.UpdateTaskPrice(
                task = TaskItem(id = "task-1"),
                price = -1.0
            )
        )
        advanceUntilIdle()

        assertNull(repository.updatedPrice)
        assertEquals("Price must be zero or greater", viewModel.uiState.value.transientErrorMessage)
    }

    @Test
    fun repositoryFailure_exposesSafeMessageAndLogsOnlyErrorType() = runTest {
        val repository = FakeBoardRepository(
            addTaskFailure = IllegalStateException("internal document path")
        )
        val viewModel = createViewModel(repository)
        collectState(viewModel)

        viewModel.onAction(BoardDetailAction.AddTask("Buy milk"))
        advanceUntilIdle()

        assertEquals("Failed to add task", viewModel.uiState.value.transientErrorMessage)
        assertEquals("addTask", repository.loggedFunctionName)
        assertEquals("IllegalStateException", repository.loggedErrorType)
    }

    @Test
    fun recentTasks_areOrderedByCreationTime() = runTest {
        val repository = FakeBoardRepository(
            initialTasks = listOf(
                TaskItem(id = "z-id", title = "Older", createdAt = 10L),
                TaskItem(id = "a-id", title = "Newer", createdAt = 20L)
            )
        )
        val viewModel = createViewModel(repository)
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(listOf("Newer", "Older"), viewModel.uiState.value.tasks.map { it.title })
    }

    private fun createViewModel(repository: BoardRepository): BoardDetailViewModel =
        BoardDetailViewModel(
            repository = repository,
            celebrationSettings = FakeCelebrationSettings(),
            savedStateHandle = SavedStateHandle(mapOf("boardId" to TEST_BOARD_ID))
        )

    private fun kotlinx.coroutines.test.TestScope.collectState(viewModel: BoardDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
    }

    private class FakeCelebrationSettings : CelebrationSettings {
        override val showCelebration: Flow<Boolean> = flowOf(true)
    }

    private class FakeBoardRepository(
        initialTasks: List<TaskItem> = emptyList(),
        private val addTaskFailure: Exception? = null
    ) : BoardRepository {
        private val board = MutableStateFlow<Board?>(
            Board(id = TEST_BOARD_ID, name = "Test board")
        )
        private val tasks = MutableStateFlow(initialTasks)
        private val members = MutableStateFlow<List<User>>(emptyList())

        var addedTitle: String? = null
        var updatedPrice: Double? = null
        var loggedFunctionName: String? = null
        var loggedErrorType: String? = null

        override fun getBoards(): Flow<List<Board>> = flowOf(emptyList())

        override suspend fun createBoard(
            name: String,
            description: String,
            type: BoardType
        ): Board = Board(name = name, description = description, type = type)

        override suspend fun updateBoard(boardId: String, name: String, description: String) = Unit

        override suspend fun joinBoard(boardId: String) = Unit

        override fun getBoard(boardId: String): Flow<Board?> = board

        override fun getBoardMembers(boardId: String): Flow<List<User>> = members

        override fun getTasks(boardId: String): Flow<List<TaskItem>> = tasks

        override suspend fun addTask(boardId: String, title: String): TaskItem {
            addTaskFailure?.let { throw it }
            addedTitle = title
            return TaskItem(id = "new-task", boardId = boardId, title = title)
        }

        override suspend fun toggleTask(boardId: String, taskId: String, isDone: Boolean) = Unit

        override suspend fun updateTaskPrice(boardId: String, taskId: String, price: Double) {
            updatedPrice = price
        }

        override suspend fun updateTaskDetails(
            boardId: String,
            taskId: String,
            notes: String,
            dueDate: Long?,
            priority: Priority
        ) = Unit

        override suspend fun assignTask(boardId: String, taskId: String, userId: String) = Unit

        override suspend fun deleteTask(boardId: String, taskId: String) = Unit

        override suspend fun deleteBoard(boardId: String) = Unit

        override suspend fun logError(
            functionName: String,
            errorMessage: String,
            boardId: String?
        ) {
            loggedFunctionName = functionName
            loggedErrorType = errorMessage
        }
    }

    private companion object {
        const val TEST_BOARD_ID = "board-1"
    }
}
