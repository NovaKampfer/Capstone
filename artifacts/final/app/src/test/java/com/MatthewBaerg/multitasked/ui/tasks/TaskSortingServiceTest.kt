package com.MatthewBaerg.multitasked.ui.tasks

import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskSortingServiceTest {
    @Test fun listWorkRunsOnInjectedWorkerThread() = runBlocking {
        lateinit var worker: Thread
        Executors.newSingleThreadExecutor { action ->
            Thread(action, "sort-test-worker").also { worker = it }
        }
            .asCoroutineDispatcher().use { dispatcher ->
                val threads = mutableListOf<Thread>()
                val input = object : AbstractList<TaskItem>() {
                    override val size = 2
                    override fun get(index: Int): TaskItem {
                        threads.add(Thread.currentThread())
                        return TaskItem(id = index.toString(), dueDate = (2 - index).toLong())
                    }
                }
                val service = TaskSortingService(dispatcher, TaskClock { 100L })
                assertEquals(listOf("1", "0"), service.sort(input, TaskSortOption.DUE_DATE).map { it.id })
                assertTrue(threads.isNotEmpty())
                // Coroutine debugging may decorate thread names; identity verifies dispatch.
                assertTrue(threads.all { it === worker })
            }
    }

    @Test fun returnedTasksDoNotAliasMutableInputFields() = runTest {
        val input = TaskItem(id = "a", createdAt = 3L)
        val service = TaskSortingService(StandardTestDispatcher(testScheduler), TaskClock { 100L })
        val result = service.sort(listOf(input), TaskSortOption.RECENT)
        result.single().createdAt = 99L
        assertEquals(3L, input.createdAt)
    }

    @Test fun changingOptionKeepsResultAndLabelTogether() = runTest {
        val service = TaskSortingService(StandardTestDispatcher(testScheduler), TaskClock { 100L })
        val tasks = MutableStateFlow(listOf(TaskItem(id = "b", createdAt = 20L, dueDate = 200L),
            TaskItem(id = "a", createdAt = 10L, dueDate = 150L)))
        val options = MutableStateFlow(TaskSortOption.RECENT)
        val results = mutableListOf<SortedTaskList>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            service.observe(tasks, options).toList(results)
        }
        runCurrent()
        assertEquals(listOf("b", "a"), results.last().tasks.map { it.id })
        options.value = TaskSortOption.DUE_DATE
        runCurrent()
        assertEquals(TaskSortOption.DUE_DATE, results.last().option)
        assertEquals(listOf("a", "b"), results.last().tasks.map { it.id })
    }

    @Test fun urgencyRefreshesAsTimePassesAndStopsWithCollector() = runTest {
        var now = 100L
        val service = TaskSortingService(StandardTestDispatcher(testScheduler), TaskClock { now })
        val tasks = flowOf(listOf(
            TaskItem(id = "low", dueDate = 150L, priority = Priority.LOW),
            TaskItem(id = "high", dueDate = 999999L, priority = Priority.HIGH)))
        val results = mutableListOf<SortedTaskList>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            service.observe(tasks, flowOf(TaskSortOption.URGENCY)).toList(results)
        }
        runCurrent()
        assertEquals("high", results.last().tasks.first().id)
        now = 151L
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals("low", results.last().tasks.first().id)
        collector.cancelAndJoin()
        val count = results.size
        advanceTimeBy(120_000L)
        runCurrent()
        assertEquals(count, results.size)
    }

    @Test fun latestTaskSnapshotWins() = runTest {
        val service = TaskSortingService(StandardTestDispatcher(testScheduler), TaskClock { 100L })
        val tasks = MutableStateFlow(listOf(TaskItem(id = "old")))
        val results = mutableListOf<SortedTaskList>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            service.observe(tasks, flowOf(TaskSortOption.DUE_DATE)).toList(results)
        }
        runCurrent()
        tasks.value = listOf(TaskItem(id = "intermediate"))
        tasks.value = listOf(TaskItem(id = "latest"))
        runCurrent()
        assertEquals(listOf("latest"), results.last().tasks.map { it.id })
    }

    @Test fun canceledSortDoesNotPublishAResult() = runTest {
        val service = TaskSortingService(StandardTestDispatcher(testScheduler), TaskClock { 100L })
        var result: List<TaskItem>? = null
        val job = launch {
            val ownJob = currentCoroutineContext().job
            val input = object : AbstractList<TaskItem>() {
                override val size = 100
                override fun get(index: Int): TaskItem {
                    if (index == 3) ownJob.cancel()
                    return TaskItem(id = index.toString())
                }
            }
            result = service.sort(input, TaskSortOption.URGENCY)
        }
        runCurrent()
        job.join()
        assertTrue(job.isCancelled)
        assertNull(result)
    }
}
