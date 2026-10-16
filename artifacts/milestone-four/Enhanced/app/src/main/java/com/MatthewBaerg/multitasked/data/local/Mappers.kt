package com.MatthewBaerg.multitasked.data.local

import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem

// Reuse the Milestone Three algorithms without giving them database dependencies.
fun TaskEntity.toTaskItem() = TaskItem(
    id = id, boardId = boardId, title = title, isDone = completed,
    createdAt = createdAt, notes = notes, dueDate = dueDate,
    priority = Priority.valueOf(priority)
)
