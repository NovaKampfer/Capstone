package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.Priority
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSortPolicyTest {
    @Test fun dueDateOptionUsesDateThenPriorityWithStableTies() {
        val tasks = listOf(TaskItem(id = "none"), TaskItem(id = "late", dueDate = 200L),
            TaskItem(id = "z", dueDate = 10L, priority = Priority.HIGH),
            TaskItem(id = "a", dueDate = 10L, priority = Priority.HIGH))
        assertEquals(listOf("z", "a", "late", "none"),
            TaskSortPolicy.sort(tasks, TaskSortOption.DUE_DATE).map { it.id })
    }

    @Test fun urgencyOptionDrainsHeapInRuleOrder() {
        val tasks = listOf(TaskItem(id = "done", isDone = true, dueDate = 1L),
            TaskItem(id = "normal", priority = Priority.HIGH),
            TaskItem(id = "late", dueDate = 1L, priority = Priority.LOW))
        assertEquals(listOf("late", "normal", "done"),
            TaskSortPolicy.sort(tasks, TaskSortOption.URGENCY, 100L).map { it.id })
    }

    @Test fun heapPathCooperatesWithCancellation() {
        var calls = 0
        assertThrows(CancellationException::class.java) {
            TaskSortPolicy.sort(List(500) { TaskItem(createdAt = (500 - it).toLong()) },
                TaskSortOption.URGENCY, 100L) {
                if (++calls == 30) throw CancellationException("test")
            }
        }
        assertEquals(30, calls)
    }

    @Test fun allOptionsAcceptEmptyInput() {
        TaskSortOption.entries.forEach {
            assertEquals(emptyList<TaskItem>(), TaskSortPolicy.sort(emptyList(), it, 100L))
        }
    }

    @Test fun originalAlphabeticAndCompletionOptionsKeepTheirMeaning() {
        val tasks = listOf(TaskItem(id = "z", title = "zebra", isDone = false),
            TaskItem(id = "a", title = "Alpha", isDone = true))
        assertEquals(listOf("a", "z"), TaskSortPolicy.sort(tasks, TaskSortOption.ALPHA_ASC).map { it.id })
        assertEquals(listOf("z", "a"), TaskSortPolicy.sort(tasks, TaskSortOption.ALPHA_DESC).map { it.id })
        assertEquals(listOf("a", "z"), TaskSortPolicy.sort(tasks, TaskSortOption.COMPLETED_FIRST).map { it.id })
    }

    @Test
    fun recentSort_usesCreationTimeInsteadOfDocumentId() {
        val olderWithLargerId = TaskItem(id = "z-id", title = "Older", createdAt = 100L)
        val newerWithSmallerId = TaskItem(id = "a-id", title = "Newer", createdAt = 200L)

        val result = TaskSortPolicy.sort(
            listOf(olderWithLargerId, newerWithSmallerId),
            TaskSortOption.RECENT
        )

        assertEquals(listOf("Newer", "Older"), result.map { it.title })
    }

    @Test
    fun incompleteFirst_usesTitleAsDeterministicTieBreaker() {
        val result = TaskSortPolicy.sort(
            listOf(
                TaskItem(id = "3", title = "Complete", isDone = true),
                TaskItem(id = "2", title = "zebra", isDone = false),
                TaskItem(id = "1", title = "Alpha", isDone = false)
            ),
            TaskSortOption.INCOMPLETE_FIRST
        )

        assertEquals(listOf("Alpha", "zebra", "Complete"), result.map { it.title })
    }
}
