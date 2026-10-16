package com.MatthewBaerg.multitasked.ui.tasks

/**
 * Stable top-down merge sort. Never reorders the caller's list.
 * O(n log n) time, O(n) auxiliary space plus O(log n) call stack.
 * checkActive lets the caller cooperate with cancellation without Android dependencies.
 */
internal object TaskMergeSorter {
    fun <T> sort(
        input: List<T>,
        comparator: Comparator<T>,
        checkActive: () -> Unit = {}
    ): List<T> {
        checkActive()
        val values = ArrayList<T>(input.size)
        for (item in input) {
            checkActive()
            values.add(item)
        }
        if (values.size < 2) return values
        val buffer = ArrayList<T>(values.size)
        for (item in values) {
            checkActive()
            buffer.add(item)
        }

        // Half-open range [start, end): end is not included.
        fun sortRange(start: Int, end: Int) {
            checkActive()
            if (end - start < 2) return
            val middle = start + (end - start) / 2
            sortRange(start, middle)
            sortRange(middle, end)
            var left = start
            var right = middle
            for (destination in start until end) {
                checkActive()
                // Choosing LEFT on equality is the stability guarantee.
                buffer[destination] = when {
                    left >= middle -> values[right++]
                    right >= end -> values[left++]
                    comparator.compare(values[left], values[right]) <= 0 -> values[left++]
                    else -> values[right++]
                }
            }
            for (index in start until end) {
                checkActive()
                values[index] = buffer[index]
            }
        }
        sortRange(0, values.size)
        return values
    }
}
