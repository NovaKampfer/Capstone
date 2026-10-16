package com.MatthewBaerg.multitasked.data.repository

import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User
import kotlinx.coroutines.flow.Flow

interface BoardRepository {
    fun getBoards(): Flow<List<Board>>
    suspend fun createBoard(name: String, description: String, type: BoardType): Board
    suspend fun updateBoard(boardId: String, name: String, description: String)
    suspend fun joinBoard(boardId: String)
    fun getBoard(boardId: String): Flow<Board?>
    fun getBoardMembers(boardId: String): Flow<List<User>>

    fun getTasks(boardId: String): Flow<List<TaskItem>>
    suspend fun addTask(boardId: String, title: String): TaskItem
    suspend fun toggleTask(boardId: String, taskId: String, isDone: Boolean)
    suspend fun updateTaskPrice(boardId: String, taskId: String, price: Double)
    suspend fun updateTaskDetails(boardId: String, taskId: String, notes: String, dueDate: Long?, priority: Priority)
    suspend fun assignTask(boardId: String, taskId: String, userId: String)

    suspend fun deleteTask(boardId: String, taskId: String)
    suspend fun deleteBoard(boardId: String)
    
    suspend fun logError(functionName: String, errorMessage: String, boardId: String?)
}
