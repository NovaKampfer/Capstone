# Milestone Three paired-programming study guide

Use this guide to study one file at a time. Do not claim that you understand every line until you can trace the examples and explain the tests. All paths below are relative to `Enhanced/app/src/main/java/com/MatthewBaerg/multitasked/` unless noted otherwise. The original and immediate baseline are preserved for comparison.

## 1. Start with the data and menu choices

Open `data/model/TaskItem.kt`, then `ui/tasks/BoardDetailContract.kt`.

`TaskItem` already stores due dates as nullable millisecond timestamps and priority as an enum. `null` means no due date. No schema change was needed. Some fields are mutable, which matters when background work uses the same objects.

The only contract change is adding `DUE_DATE` and `URGENCY` to `TaskSortOption`. An enum represents a finite set of choices, so Kotlin's exhaustive `when` can expose missing handlers at compile time.

Explain aloud: Why is `null` different from zero? Why should a sorting change avoid rewriting the database model unnecessarily?

## 2. Define what earlier means

Open `ui/tasks/TaskUrgency.kt`.

- `internal object` creates one package-module-visible policy object. It has no Android lifecycle dependency.
- `rank` assigns an explicit integer to each priority. This avoids silently changing business rules if the enum is reordered later.
- `compareDates` handles both-null, left-null, right-null, and both-real cases. `compareTo` avoids the overflow risk of subtracting two extreme timestamps.
- `dueDateComparator` returns the date comparison unless it is zero. Zero means equal, so priority becomes the next comparison.
- `comparator(nowMillis)` builds a sequence of comparisons. A negative result means the left task comes first. `thenBy` is consulted only when the preceding keys tie.
- `isOverdue` excludes completed tasks and uses strict `<`. Due exactly at the captured current time is not yet overdue.

The urgency rule is lexicographic, not a weighted numeric score. For example, with current time 100, a LOW task due at 90 comes before a HIGH task due at 150 because overdue status is considered before priority. A completed task comes after both. With equal overdue status and priority, the earlier date wins.

Explain aloud: Why must `nowMillis` remain fixed during one sort? How would a clock read on every comparison threaten consistent ordering?

## 3. Trace the stable merge sort

Open `ui/tasks/TaskMergeSorter.kt`.

`<T>` makes the algorithm generic: it can sort tasks or the small records used in tests. The comparator defines ordering without coupling the algorithm to a task field. `checkActive` is an injected callback; its default does nothing for ordinary synchronous callers, while the service supplies a coroutine cancellation check.

The first loop copies list references into `values`. The second allocates a reusable merge buffer. These lists use O(n) extra memory. The algorithm itself does not deep-copy arbitrary objects; task snapshots are made by the service.

`sortRange(start, end)` uses a half-open range: start is included and end is excluded. A range smaller than two is already sorted. The midpoint formula avoids adding two potentially large indexes. Two recursive calls sort each half before the merge loop begins.

`left` and `right` track the next unread element in each half. The first two branches handle exhausted halves. The comparison branch uses `<=`, choosing the left value when the keys tie. That one equality decision preserves the relative order of equal keys. The last loop copies the merged range back into `values` for the parent call.

Trace four cards: A due 20, B due 10, C due 20, D due 5, all with the same priority. The result is D, B, A, C. A remains before C. There are logarithmically many split levels, with linear merge work per level, so time is O(n log n).

Explain aloud: What would change if `<=` became `<`? Why can the buffer be reused safely after each recursive call returns?

## 4. Trace the binary heap

Open `ui/tasks/TaskPriorityQueue.kt`.

An ArrayList stores a complete binary tree without allocating tree nodes. For index i, the left child is 2i + 1 and the right child is 2i + 2. For a non-root child, the parent is (i - 1) / 2. The invariant is that no parent compares greater than either child. This guarantees that the root is the next task, but the underlying array is not globally sorted.

`offer` appends a copied task and moves it upward while it outranks its parent. `peek` returns a copy of the root without removal. `poll` removes the root, moves the last item into its position, and calls `siftDown`. `siftDown` chooses the better child, then swaps only when the parent is worse. The first `size / 2` positions are the only positions with children.

Copying on entry and exit prevents callers from changing mutable ordering fields inside the heap. The queue is local to one sort, so it needs no lock. Reusing it across threads would require a new ownership or synchronization design.

Trace integer priorities 3, 1, 2: after insertion the root is 1; repeated removal yields 1, 2, 3. In the application, the comparator uses the full task rule rather than a single number. Heap insertion is O(log n) amortized, removal O(log n), and peek O(1). Repeated insertion plus removing every task is O(n log n); it is not a constant-time full sort.

Explain aloud: Why must removal choose the better child? Why can changing a stored task's priority invalidate the heap without moving any array elements?

## 5. Follow the algorithm selection

Open `ui/tasks/TaskSortPolicy.kt`.

The policy directs DUE_DATE to merge sort and URGENCY to the heap. The urgency branch creates a fresh comparator and queue, inserts the current tasks, then drains the queue into a display list. The other choices use the existing standard-library comparators. The added checked comparator lets those existing modes respond to cancellation too.

Two separate algorithms are used deliberately; running merge sort and heap sort on the same complete list would duplicate work. A future scheduler could retain a heap for repeated next-task retrieval, but this implementation rebuilds it for each snapshot so edited dates and priorities cannot leave stale heap entries.

Explain aloud: Why is the custom heap educationally useful even though a full drain has the same asymptotic time as merge sort? What maintenance cost would a standard library queue avoid?

## 6. Follow the background work

Open `ui/tasks/TaskClock.kt`, `ui/tasks/TaskSortingService.kt`, `di/SortingDispatcher.kt`, and `di/AppModule.kt`.

`TaskClock` is a small interface that makes time controllable in tests. The dispatcher qualifier tells Hilt which CoroutineDispatcher to inject. AppModule supplies `Dispatchers.Default`, appropriate for CPU work. Declaring a function `suspend` alone would not move sorting off the main thread; `withContext(dispatcher)` performs that context switch.

Within `sort`, a snapshot copies mutable task fields before ordering. `currentCoroutineContext().ensureActive()` throws when the job has been canceled. These checks inside loops make cancellation cooperative, so CPU work does not keep running until the entire old sort completes.

Within `observe`, `flatMapLatest` switches the whole pipeline when the selected option changes. `combine` pairs the latest tasks with a timestamp. `mapLatest` cancels an unfinished sort when a newer input arrives. `SortedTaskList` carries the option and its result together. Urgency has a 60-second clock flow; other modes need no periodic refresh. Canceling collection cancels the clock loop too.

The service assumes repository emissions are snapshots that are not concurrently mutated. Copying mitigates shared mutable fields but cannot make an arbitrary concurrent input mutation safe. A fully immutable task model is a possible later improvement.

Explain aloud: Why is Default used instead of IO? Why are clock and dispatcher injected rather than hardcoded? What is the difference between starting background work and making it cancelable?

## 7. Connect to the screen

Open `ui/tasks/BoardDetailViewModel.kt`, then `ui/tasks/BoardDetailScreen.kt`.

The ViewModel receives the service through its constructor. Its combined state now receives sorted results instead of invoking sorting directly. Changes to board-member or celebration state no longer repeat a sort by themselves. `stateIn` shares state in the ViewModel scope, and the existing WhileSubscribed timeout can keep the flow alive for five seconds after the last subscriber leaves.

The screen's existing menu iterates through all enum values. The `displayName` mapping supplies readable names for the two new options. The screen still sends actions rather than implementing the algorithms.

Explain aloud: Why should the composable not own the heap? Why travel with the matching sort label instead of reading a separate label after background work completes?

## 8. Read the tests as examples

Open `app/src/test/java/com/MatthewBaerg/multitasked/ui/tasks/` under Enhanced.

Read in this order: TaskUrgencyTest, TaskMergeSorterTest, TaskPriorityQueueTest, TaskSortPolicyTest, TaskSortingServiceTest, BoardDetailViewModelTest. Keep BoardDetailUiStateTest as regression coverage for summary calculations. The first five plus BoardDetailUiStateTest were executed in the standalone run; ViewModel tests still require the Android build environment.

For each test, identify the arrangement, the operation, and the expected result. Randomized tests use fixed seeds so failures are reproducible. Comparison-count tests support the complexity discussion without confusing device timing with asymptotic behavior. They do not constitute a mathematical proof for every input.

Final learning check: Explain the ordering rules without opening the code, trace one merge and one heap removal by hand, and describe how a canceled sort exits. Then run the full Android checks in README before submission.
