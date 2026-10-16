package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSortPolicyTest {
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
