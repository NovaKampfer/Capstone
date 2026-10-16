package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem

/** Business rules shared by the algorithms, independent of Android and wall-clock reads. */
internal object TaskUrgency {
    // Explicit ranks avoid coupling the rule to the enum's declaration order.
    fun rank(priority: Priority): Int = when (priority) {
        Priority.HIGH -> 0
        Priority.MEDIUM -> 1
        Priority.LOW -> 2
        Priority.NONE -> 3
    }

    /** null is later than EVERY real timestamp, including Long.MAX_VALUE. */
    fun compareDates(left: Long?, right: Long?): Int = when {
        left == null && right == null -> 0
        left == null -> 1
        right == null -> -1
        else -> left.compareTo(right) // Do not subtract: subtraction can overflow.
    }

    /** Date order, then urgency level; exact ties retain input order in merge sort. */
    val dueDateComparator: Comparator<TaskItem> = Comparator { left, right ->
        val dateOrder = compareDates(left.dueDate, right.dueDate)
        if (dateOrder != 0) dateOrder else rank(left.priority).compareTo(rank(right.priority))
    }

    /**
     * Incomplete before complete; overdue before non-overdue; HIGH before NONE;
     * then earlier date, older creation time, and ID. Capture now ONCE per sort.
     * A task due exactly at now is due, but is not yet overdue.
     */
    fun comparator(nowMillis: Long): Comparator<TaskItem> =
        compareBy<TaskItem> { it.isDone }
            .thenBy { task -> if (isOverdue(task, nowMillis)) 0 else 1 }
            .thenBy { rank(it.priority) }
            .thenComparator { left, right -> compareDates(left.dueDate, right.dueDate) }
            .thenBy { it.createdAt }
            .thenBy { it.id }

    private fun isOverdue(task: TaskItem, nowMillis: Long): Boolean =
        !task.isDone && task.dueDate?.let { it < nowMillis } == true
}
