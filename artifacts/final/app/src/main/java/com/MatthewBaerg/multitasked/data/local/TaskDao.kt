package com.MatthewBaerg.multitasked.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM local_tasks WHERE boardId = :boardId ORDER BY createdAt DESC, id ASC")
    fun observeTasks(boardId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM local_tasks WHERE boardId = :boardId AND id = :id")
    suspend fun get(boardId: String, id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity)

    @Query("UPDATE local_tasks SET title = :title, notes = :notes, dueDate = :dueDate, priority = :priority WHERE boardId = :boardId AND id = :id")
    suspend fun updateDetails(boardId: String, id: String, title: String, notes: String,
                              dueDate: Long?, priority: String): Int

    @Query("UPDATE local_tasks SET completed = :completed WHERE boardId = :boardId AND id = :id")
    suspend fun setCompleted(boardId: String, id: String, completed: Boolean): Int

    @Query("DELETE FROM local_tasks WHERE boardId = :boardId AND id = :id")
    suspend fun delete(boardId: String, id: String): Int
}
