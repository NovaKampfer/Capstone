package com.MatthewBaerg.multitasked.data.repository

import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User
import com.MatthewBaerg.multitasked.data.remote.FirebaseDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardRepositoryImpl @Inject constructor(
    private val remote: FirebaseDataSource
) : BoardRepository {

    override fun getBoards(): Flow<List<Board>> =
        remote.getBoardsForCurrentUser()

    override suspend fun createBoard(name: String, description: String, type: BoardType): Board =
        remote.createBoard(name, description, type)

    override suspend fun updateBoard(boardId: String, name: String, description: String) {
        remote.updateBoard(boardId, name, description)
    }

    override suspend fun joinBoard(boardId: String) =
        remote.joinBoardById(boardId)

    override fun getBoard(boardId: String): Flow<Board?> =
        remote.getBoard(boardId)

    override fun getBoardMembers(boardId: String): Flow<List<User>> =
        remote.getBoardMembers(boardId)

    override fun getTasks(boardId: String): Flow<List<TaskItem>> =
        remote.getTasksForBoard(boardId)

    override suspend fun addTask(boardId: String, title: String): TaskItem =
        remote.addTask(boardId, title)

    override suspend fun toggleTask(
        boardId: String,
        taskId: String,
        isDone: Boolean
    ) = remote.toggleTaskDone(boardId, taskId, isDone)

    override suspend fun updateTaskPrice(boardId: String, taskId: String, price: Double) {
        remote.updateTaskPrice(boardId, taskId, price)
    }

    override suspend fun updateTaskDetails(boardId: String, taskId: String, notes: String, dueDate: Long?, priority: Priority) {
        remote.updateTaskDetails(boardId, taskId, notes, dueDate, priority)
    }

    override suspend fun assignTask(boardId: String, taskId: String, userId: String) {
        remote.assignTask(boardId, taskId, userId)
    }

    override suspend fun deleteTask(boardId: String, taskId: String) =
        remote.deleteTask(boardId, taskId)

    override suspend fun deleteBoard(boardId: String) =
        remote.deleteBoard(boardId)

    override suspend fun logError(functionName: String, errorMessage: String, boardId: String?) {
        remote.logError(functionName, errorMessage, boardId)
    }
}
