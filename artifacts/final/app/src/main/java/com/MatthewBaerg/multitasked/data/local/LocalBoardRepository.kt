package com.MatthewBaerg.multitasked.data.local

import com.MatthewBaerg.multitasked.data.model.Priority
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

/** Device-scoped storage. No account identity or cloud synchronization is implied. */
@Singleton
class LocalBoardRepository @Inject constructor(private val database: AppDatabase) {
    private val boards = database.boardDao()
    private val tasks = database.taskDao()

    fun observeBoards() = boards.observeBoards()
    fun observeBoard(id: String) = boards.observeBoard(id)
    fun observeTasks(boardId: String) = tasks.observeTasks(boardId).map { rows ->
        rows.map { it.toTaskItem() }
    }

    suspend fun createBoard(name: String, firstTaskTitle: String = "") {
        val board = BoardEntity(UUID.randomUUID().toString(), validTitle(name), System.currentTimeMillis())
        val firstTasks = if (firstTaskTitle.isBlank()) emptyList() else listOf(
            TaskEntity(UUID.randomUUID().toString(), board.id, validTitle(firstTaskTitle),
                false, System.currentTimeMillis())
        )
        boards.createWithTasks(board, firstTasks)
    }

    suspend fun renameBoard(id: String, name: String) {
        require(boards.rename(id, validTitle(name)) == 1) { "Board no longer exists" }
    }

    suspend fun deleteBoard(id: String) {
        require(boards.delete(id) == 1) { "Board no longer exists" }
    }

    suspend fun saveTask(boardId: String, id: String?, title: String, notes: String,
                         dueDate: Long?, priority: Priority) {
        val cleanTitle = validTitle(title)
        require(notes.length <= 2000) { "Notes must be 2000 characters or fewer" }
        if (id == null) {
            tasks.insert(TaskEntity(UUID.randomUUID().toString(), boardId, cleanTitle, false,
                System.currentTimeMillis(), notes.trim(), dueDate, priority.name))
        } else {
            require(tasks.updateDetails(boardId, id, cleanTitle, notes.trim(), dueDate, priority.name) == 1) {
                "Task no longer exists"
            }
        }
    }

    suspend fun setCompleted(boardId: String, id: String, completed: Boolean) {
        require(tasks.setCompleted(boardId, id, completed) == 1) { "Task no longer exists" }
    }

    suspend fun deleteTask(boardId: String, id: String) {
        require(tasks.delete(boardId, id) == 1) { "Task no longer exists" }
    }

    private fun validTitle(value: String): String = value.trim().also {
        require(it.isNotEmpty() && it.length <= 120) { "Name or title must be 1 to 120 characters" }
    }
}
