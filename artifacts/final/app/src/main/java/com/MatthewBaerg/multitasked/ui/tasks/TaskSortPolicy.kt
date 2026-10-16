package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem
import java.util.Locale

/**
 * Chooses an algorithm without placing business logic inside a composable.
 * Existing sort meanings are preserved; new modes use custom implementations.
 */
internal object TaskSortPolicy {
    fun sort(
        tasks: List<TaskItem>,
        option: TaskSortOption,
        nowMillis: Long = System.currentTimeMillis(),
        checkActive: () -> Unit = {}
    ): List<TaskItem> {
        checkActive()
        if (option == TaskSortOption.DUE_DATE) {
            return TaskMergeSorter.sort(tasks, TaskUrgency.dueDateComparator, checkActive)
        }
        if (option == TaskSortOption.URGENCY) {
            val ordering = TaskUrgency.comparator(nowMillis)
            val queue = TaskPriorityQueue(Comparator { left, right ->
                checkActive()
                ordering.compare(left, right)
            })
            for (task in tasks) {
                checkActive()
                queue.offer(task)
            }
            val result = ArrayList<TaskItem>(tasks.size)
            while (!queue.isEmpty()) {
                checkActive()
                result.add(checkNotNull(queue.poll()))
            }
            return result
        }
        val comparator = when (option) {
            TaskSortOption.RECENT ->
                    compareByDescending<TaskItem> { it.createdAt }
                        .thenBy { it.id }

            TaskSortOption.ALPHA_ASC ->
                    compareBy<TaskItem> { it.title.lowercase(Locale.ROOT) }
                        .thenByDescending { it.createdAt }
                        .thenBy { it.id }

            TaskSortOption.ALPHA_DESC ->
                    compareByDescending<TaskItem> { it.title.lowercase(Locale.ROOT) }
                        .thenByDescending { it.createdAt }
                        .thenBy { it.id }

            TaskSortOption.COMPLETED_FIRST ->
                    compareByDescending<TaskItem> { it.isDone }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                        .thenBy { it.id }

            TaskSortOption.INCOMPLETE_FIRST ->
                    compareBy<TaskItem> { it.isDone }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                        .thenBy { it.id }
            TaskSortOption.DUE_DATE, TaskSortOption.URGENCY -> error("Handled above")
        }
        return tasks.sortedWith(Comparator { left, right ->
            checkActive()
            comparator.compare(left, right)
        })
    }
}
