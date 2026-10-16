package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardDetailUiStateTest {
    @Test
    fun summaryValues_areCalculatedFromTasks() {
        val state = BoardDetailUiState(
            tasks = listOf(
                TaskItem(id = "1", isDone = true, price = 3.25),
                TaskItem(id = "2", isDone = false, price = 4.75)
            )
        )

        assertEquals(2, state.totalTasks)
        assertEquals(1, state.completedTasks)
        assertEquals(0.5f, state.progress, 0.001f)
        assertEquals(8.0, state.totalCost, 0.001)
    }

    @Test
    fun emptyTaskList_hasZeroProgress() {
        val state = BoardDetailUiState()

        assertEquals(0f, state.progress, 0.001f)
    }
}
