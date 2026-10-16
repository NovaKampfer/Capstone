package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class TaskUrgencyTest {
    private val now = 100L
    private val order = TaskUrgency.comparator(now)
    private fun sorted(vararg tasks: TaskItem) = tasks.toList().sortedWith(order).map { it.id }

    @Test fun incompleteBeforeCompletedEvenIfCompletedIsOverdue() {
        assertEquals(listOf("open", "done"), sorted(
            TaskItem(id = "done", isDone = true, dueDate = 1L, priority = Priority.HIGH),
            TaskItem(id = "open")
        ))
    }

    @Test fun overdueBeforeUpcomingRegardlessOfPriority() {
        assertEquals(listOf("late", "future"), sorted(
            TaskItem(id = "future", dueDate = 200L, priority = Priority.HIGH),
            TaskItem(id = "late", dueDate = 99L, priority = Priority.LOW)
        ))
    }

    @Test fun priorityIsUsedWithinSameOverdueGroup() {
        val input = Priority.entries.reversed().map { TaskItem(id = it.name, priority = it) }
        assertEquals(listOf("HIGH", "MEDIUM", "LOW", "NONE"), input.sortedWith(order).map { it.id })
    }

    @Test fun equalPriorityUsesDateThenCreationThenId() {
        assertEquals(listOf("early", "a", "b", "new", "undated"), sorted(
            TaskItem(id = "undated"),
            TaskItem(id = "new", dueDate = 200L, createdAt = 2L),
            TaskItem(id = "b", dueDate = 200L, createdAt = 1L),
            TaskItem(id = "a", dueDate = 200L, createdAt = 1L),
            TaskItem(id = "early", dueDate = 150L, createdAt = 999L)
        ))
    }

    @Test fun exactlyNowIsNotOverdue() {
        assertEquals(listOf("high", "due-now"), sorted(
            TaskItem(id = "due-now", dueDate = now, priority = Priority.LOW),
            TaskItem(id = "high", dueDate = 200L, priority = Priority.HIGH)
        ))
    }

    @Test fun extremeDatesAndNullAreComparedWithoutOverflow() {
        val input = listOf(TaskItem(id = "null"), TaskItem(id = "max", dueDate = Long.MAX_VALUE),
            TaskItem(id = "min", dueDate = Long.MIN_VALUE))
        assertEquals(listOf("min", "max", "null"), input.sortedWith(TaskUrgency.dueDateComparator).map { it.id })
    }

    @Test fun dueDateTiesUsePriorityButExactTiesRemainStable() {
        val input = listOf(
            TaskItem(id = "z", dueDate = 10L, priority = Priority.HIGH),
            TaskItem(id = "low", dueDate = 10L, priority = Priority.LOW),
            TaskItem(id = "a", dueDate = 10L, priority = Priority.HIGH))
        val result = TaskMergeSorter.sort(input, TaskUrgency.dueDateComparator)
        assertEquals(listOf("z", "a", "low"), result.map { it.id })
    }

    @Test fun urgencyComparatorIsAntisymmetricAndTransitive() {
        val random = Random(499)
        fun task() = TaskItem(id = random.nextInt(10).toString(), isDone = random.nextBoolean(),
            dueDate = if (random.nextBoolean()) null else random.nextLong(0L, 201L),
            priority = Priority.entries[random.nextInt(4)], createdAt = random.nextLong())
        repeat(2000) {
            val a = task(); val b = task(); val c = task()
            assertEquals(order.compare(a, b).compareTo(0), -order.compare(b, a).compareTo(0))
            if (order.compare(a, b) <= 0 && order.compare(b, c) <= 0) {
                assertTrue(order.compare(a, c) <= 0)
            }
        }
    }
}
