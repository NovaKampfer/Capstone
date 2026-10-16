package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem

/**
 * Array-backed min-heap: a parent never compares greater than either child.
 * peek O(1), poll O(log n), offer O(log n) amortized (ArrayList may grow).
 * Storage O(n). This queue is confined to one sort, never shared between threads.
 * The comparator must stay constant while the queue contains tasks.
 */
internal class TaskPriorityQueue(private val comparator: Comparator<TaskItem>) {
    private val heap = ArrayList<TaskItem>()
    val size: Int get() = heap.size
    fun isEmpty(): Boolean = heap.isEmpty()

    // Copy on entry/exit because the legacy model has mutable isDone and createdAt fields.
    fun peek(): TaskItem? = heap.firstOrNull()?.copy()

    fun offer(task: TaskItem) {
        heap.add(task.copy())
        var child = heap.lastIndex
        while (child > 0) {
            val parent = (child - 1) / 2
            if (comparator.compare(heap[parent], heap[child]) <= 0) break
            swap(parent, child)
            child = parent
        }
    }

    fun poll(): TaskItem? {
        if (heap.isEmpty()) return null
        val first = heap[0]
        val last = heap.removeAt(heap.lastIndex)
        if (heap.isNotEmpty()) {
            heap[0] = last
            siftDown()
        }
        return first.copy()
    }

    private fun siftDown() {
        var parent = 0
        // Only the first size / 2 nodes have children. This also avoids index overflow.
        while (parent < heap.size / 2) {
            val left = 2 * parent + 1
            val right = left + 1
            var best = left
            if (right < heap.size && comparator.compare(heap[right], heap[left]) < 0) {
                best = right
            }
            if (comparator.compare(heap[parent], heap[best]) <= 0) break
            swap(parent, best)
            parent = best
        }
    }

    private fun swap(first: Int, second: Int) {
        val saved = heap[first]
        heap[first] = heap[second]
        heap[second] = saved
    }
}
