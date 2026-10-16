package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.di.SortingDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/** The selected option and sorted list travel together so the label matches the results. */
data class SortedTaskList(val tasks: List<TaskItem>, val option: TaskSortOption)

/** Keeps CPU work off Main, cancels stale requests, and refreshes overdue status while observed. */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskSortingService @Inject constructor(
    @SortingDispatcher private val dispatcher: CoroutineDispatcher,
    private val clock: TaskClock
) {
    fun observe(tasks: Flow<List<TaskItem>>, options: Flow<TaskSortOption>): Flow<SortedTaskList> =
        options.flatMapLatest { option ->
            val times = if (option == TaskSortOption.URGENCY) {
                flow {
                    while (true) {
                        emit(clock.nowMillis())
                        delay(60_000L)
                    }
                }
            } else {
                flowOf(clock.nowMillis())
            }
            combine(tasks, times) { items, now -> items to now }
                .mapLatest { (items, now) ->
                    SortedTaskList(sort(items, option, now), option)
                }
        }

    suspend fun sort(
        tasks: List<TaskItem>,
        option: TaskSortOption,
        nowMillis: Long = clock.nowMillis()
    ): List<TaskItem> = withContext(dispatcher) {
        val context = currentCoroutineContext()
        // Copy mutable legacy model fields once before comparing. Repository emissions
        // are treated as snapshots and must not be mutated concurrently by callers.
        val snapshot = ArrayList<TaskItem>(tasks.size)
        for (task in tasks) {
            context.ensureActive()
            snapshot.add(task.copy())
        }
        TaskSortPolicy.sort(snapshot, option, nowMillis) { context.ensureActive() }
    }
}
