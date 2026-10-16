package com.MatthewBaerg.multitasked.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BoardDao {
    @Query("SELECT b.id, b.name, COUNT(t.id) AS taskCount FROM local_boards b LEFT JOIN local_tasks t ON t.boardId = b.id GROUP BY b.id, b.name, b.createdAt ORDER BY b.createdAt DESC, b.id ASC")
    abstract fun observeBoards(): Flow<List<BoardSummary>>

    @Query("SELECT * FROM local_boards WHERE id = :id")
    abstract fun observeBoard(id: String): Flow<BoardEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(board: BoardEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertTasks(tasks: List<TaskEntity>)

    // Used when the creation form includes an optional first task. All or nothing.
    @Transaction
    open suspend fun createWithTasks(board: BoardEntity, tasks: List<TaskEntity>) {
        require(tasks.all { it.boardId == board.id })
        insert(board)
        insertTasks(tasks)
    }

    @Query("UPDATE local_boards SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: String, name: String): Int

    // Foreign-key cascade deletes its tasks in the same SQLite statement.
    @Query("DELETE FROM local_boards WHERE id = :id")
    abstract suspend fun delete(id: String): Int
}
