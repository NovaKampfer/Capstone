package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem
import java.util.PriorityQueue
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class TaskPriorityQueueTest {
    private val order = compareBy<TaskItem> { it.createdAt }.thenBy { it.id }

    @Test fun emptyQueueReturnsNull() {
        val queue = TaskPriorityQueue(order)
        assertNull(queue.peek())
        assertNull(queue.poll())
        assertTrue(queue.isEmpty())
        assertEquals(0, queue.size)
    }

    @Test fun singleItemCanBePeekedThenRemoved() {
        val queue = TaskPriorityQueue(order)
        queue.offer(TaskItem(id = "a"))
        assertEquals("a", queue.peek()?.id)
        assertEquals(1, queue.size)
        assertEquals("a", queue.poll()?.id)
        assertNull(queue.poll())
    }

    @Test fun bothChildrenAndSingleChildAreHandled() {
        val queue = TaskPriorityQueue(order)
        listOf(9L, 4L, 8L, 1L, 3L, 2L).forEach { queue.offer(TaskItem(createdAt = it)) }
        val result = generateSequence { queue.poll() }.map { it.createdAt }.toList()
        assertEquals(listOf(1L, 2L, 3L, 4L, 8L, 9L), result)
    }

    @Test fun duplicateEntriesAreNotDropped() {
        val queue = TaskPriorityQueue(order)
        repeat(6) { queue.offer(TaskItem(id = "same")) }
        assertEquals(6, generateSequence { queue.poll() }.count())
    }

    @Test fun mutationOfInputOrPeekDoesNotCorruptHeapKeys() {
        val queue = TaskPriorityQueue(order)
        val input = TaskItem(id = "first", createdAt = 1L)
        queue.offer(input)
        queue.offer(TaskItem(id = "second", createdAt = 2L))
        input.createdAt = 999L
        queue.peek()!!.createdAt = 999L
        assertEquals("first", queue.poll()?.id)
        assertEquals("second", queue.poll()?.id)
    }

    @Test fun randomizedInterleavingMatchesJdkPriorityQueue() {
        val random = Random(499)
        val actual = TaskPriorityQueue(order)
        val expected = PriorityQueue(order)
        repeat(2000) { index ->
            if (expected.isEmpty() || random.nextBoolean()) {
                val task = TaskItem(id = index.toString(), createdAt = random.nextLong(-500L, 501L))
                actual.offer(task)
                expected.offer(task)
            } else {
                assertEquals(expected.poll(), actual.poll())
            }
            assertEquals(expected.size, actual.size)
            assertEquals(expected.peek(), actual.peek())
        }
        while (expected.isNotEmpty()) assertEquals(expected.poll(), actual.poll())
        assertTrue(actual.isEmpty())
    }

    @Test fun insertionAndDrainHaveNLogNComparisonBound() {
        var comparisons = 0
        val queue = TaskPriorityQueue(Comparator { a, b -> comparisons++; order.compare(a, b) })
        for (value in 4096 downTo 1) queue.offer(TaskItem(createdAt = value.toLong()))
        while (!queue.isEmpty()) queue.poll()
        assertTrue(comparisons <= 3 * 4096 * 12)
    }
}
