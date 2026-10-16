package com.MatthewBaerg.multitasked.ui.tasks

import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class TaskMergeSorterTest {
    private val order = Comparator<Int> { left, right -> left.compareTo(right) }

    @Test fun emptyInputProducesEmptyOutput() = assertEquals(emptyList<Int>(), TaskMergeSorter.sort(emptyList(), order))

    @Test fun singleItem() = assertEquals(listOf(4), TaskMergeSorter.sort(listOf(4), order))

    @Test fun reverseSortedWithDuplicates() = assertEquals(
        listOf(-1, 2, 2, 4, 5), TaskMergeSorter.sort(listOf(5, 4, 2, 2, -1), order)
    )

    @Test fun inputIsNotReordered() {
        val input = mutableListOf(3, 1, 2)
        val result = TaskMergeSorter.sort(input, order)
        assertEquals(listOf(3, 1, 2), input)
        assertEquals(listOf(1, 2, 3), result)
        assertNotSame(input, result)
    }

    @Test fun equalKeysRemainStableAcrossMergeBoundaries() {
        data class Record(val key: Int, val id: String)
        val input = listOf(Record(2, "a"), Record(1, "b"), Record(2, "c"), Record(1, "d"))
        val result = TaskMergeSorter.sort(input, compareBy { it.key })
        assertEquals(listOf("b", "d", "a", "c"), result.map { it.id })
    }

    @Test fun randomizedResultsMatchIndependentStandardSort() {
        val random = Random(499)
        repeat(100) {
            val input = List(random.nextInt(0, 500)) { random.nextInt(-50, 51) }
            assertEquals(input.sorted(), TaskMergeSorter.sort(input, order))
        }
    }

    @Test fun comparisonCountStaysWithinNLogNBound() {
        val input = (4096 downTo 1).toList()
        var comparisons = 0
        TaskMergeSorter.sort(input, Comparator { a, b -> comparisons++; a.compareTo(b) })
        assertTrue(comparisons <= 4096 * 12)
    }

    @Test fun cancellationStopsWork() {
        var checkpoints = 0
        assertThrows(CancellationException::class.java) {
            TaskMergeSorter.sort((100 downTo 1).toList(), order) {
                if (++checkpoints == 230) throw CancellationException("cancel test")
            }
        }
        assertEquals(230, checkpoints)
    }
}
