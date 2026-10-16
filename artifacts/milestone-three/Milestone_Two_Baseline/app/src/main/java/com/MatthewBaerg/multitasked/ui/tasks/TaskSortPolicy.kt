package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem
import java.util.Locale

/**
 * Defines the existing screen-level sort choices in one testable location.
 * The custom priority algorithm planned for the next milestone will replace
 * or extend this policy without adding business logic to the composable.
 */
internal object TaskSortPolicy {
    fun sort(tasks: List<TaskItem>, option: TaskSortOption): List<TaskItem> =
        when (option) {
            TaskSortOption.RECENT ->
                tasks.sortedWith(
                    compareByDescending<TaskItem> { it.createdAt }
                        .thenBy { it.id }
                )

            TaskSortOption.ALPHA_ASC ->
                tasks.sortedWith(
                    compareBy<TaskItem> { it.title.lowercase(Locale.ROOT) }
                        .thenByDescending { it.createdAt }
                        .thenBy { it.id }
                )

            TaskSortOption.ALPHA_DESC ->
                tasks.sortedWith(
                    compareByDescending<TaskItem> { it.title.lowercase(Locale.ROOT) }
                        .thenByDescending { it.createdAt }
                        .thenBy { it.id }
                )

            TaskSortOption.COMPLETED_FIRST ->
                tasks.sortedWith(
                    compareByDescending<TaskItem> { it.isDone }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                        .thenBy { it.id }
                )

            TaskSortOption.INCOMPLETE_FIRST ->
                tasks.sortedWith(
                    compareBy<TaskItem> { it.isDone }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                        .thenBy { it.id }
                )
        }
}
