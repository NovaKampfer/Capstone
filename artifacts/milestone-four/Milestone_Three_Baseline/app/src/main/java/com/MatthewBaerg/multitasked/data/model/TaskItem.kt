package com.MatthewBaerg.multitasked.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents a task belonging to a board.
 * Stored under: boards/{boardId}/tasks/{taskId}
 */
data class TaskItem(
    val id: String = "",
    val boardId: String = "",
    val title: String = "",

    @get:PropertyName("isDone")
    @set:PropertyName("isDone")
    var isDone: Boolean = false,

    val createdBy: String = "",

    val price: Double = 0.0,

    // For "Recent" sorting. Stored as a number (timestamp millis).
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = 0L,

    // Richer Task Details
    val notes: String = "",
    val dueDate: Long? = null, // Nullable timestamp
    val priority: Priority = Priority.NONE,

    // User assignment
    val assignedTo: String? = null
)

enum class Priority {
    HIGH,
    MEDIUM,
    LOW,
    NONE
}
