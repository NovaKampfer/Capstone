# MultiTasked — CS 499 Milestone Three

Matthew Baerg • Algorithms and Data Structures • September 27, 2026

This submission adds a custom stable merge sort, a custom binary-heap priority queue, and background coroutine sorting to the task list. The database implementation is unchanged in this milestone.

## Package contents

| Folder | Purpose |
| --- | --- |
| `Original` | Unchanged original source from the earlier submission |
| `Milestone_Two_Baseline` | Unchanged Milestone Two enhanced project, used as the immediate comparison baseline |
| `Enhanced` | Milestone Three Android project to open in Android Studio |
| `Evidence` | Actual standalone JVM test output and validation scope |
| `verification` | Reproducible supplemental JVM verification script |

Read `LEARNING_GUIDE.md` for the file order, algorithm walkthrough, and study questions. Earlier README files inside the preserved projects describe earlier milestones; this root README and `Enhanced/MILESTONE_THREE_README.md` describe this submission.

## What changed

- **Due date:** custom stable merge sort orders tasks by ascending due date, then priority. Missing dates sort after every real date. Equal date and priority retain their input order.
- **Urgency:** a custom min-heap orders incomplete tasks before completed tasks, overdue before non-overdue, then HIGH, MEDIUM, LOW, NONE, then ascending due date, creation time, and ID. Missing dates sort last within the preceding rule groups. Completed tasks are never considered overdue.
- **Background processing:** `TaskSortingService` moves sorting to an injected dispatcher supplied as `Dispatchers.Default`. It cancels superseded work and refreshes urgency every 60 seconds while observed. The timestamp is fixed for each sort.
- **Integration:** the existing sort menu exposes both choices. The ViewModel consumes a sorted list together with its matching selection label. Existing sort modes retain their ordering meanings.
- **Testing:** automated tests cover stability, duplicates, null/extreme dates, heap invariants through randomized operations, comparison counts, cancellation, worker-thread execution, and changes to sort options and task snapshots.

## Validation status

**Passed:** 38 JUnit tests in six suites compiled directly from production Kotlin sources with Kotlin 2.0.21, coroutines 1.9.0, and JUnit 4.13.2. See `Evidence/JVM_TEST_RESULTS.txt`.

**Not yet verified:** the complete Android Gradle build, Hilt generation, six ViewModel tests, and emulator/device behavior. This preparation environment does not have the Android SDK, and the Gradle wrapper download failed. Passing standalone tests does not establish that the entire app builds or runs. Earlier Milestone Two build screenshots do not validate these new changes.

## Final checks on your computer

1. Extract the ZIP. Open **Enhanced**, not the enclosing package folder, in Android Studio.
2. Use the project's existing Gradle/JDK configuration. The project uses JVM target 11, a JDK 17-compatible build toolchain, and Android SDK 36. Keep the supplied Gradle wrapper and dependency versions unless a concrete build issue requires a change.
3. In the Enhanced project terminal on Windows, run:

   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest
   ```

   If `JAVA_HOME` is missing, select the Android Studio Gradle JDK and configure the terminal to use a compatible installed JDK. Do not change Kotlin application code to solve a terminal Java-path problem.

4. Open `app/build/reports/tests/testDebugUnitTest/index.html`. There are 44 test methods in the seven task-related suites; check the actual report for discovered tests, failures, or skipped work. Do not record a passing result until Gradle completes.
5. Run on an emulator or device. In a test board you own, create tasks with different priorities, past/future dates, and no date. Select **Due date** and **Urgency**. Confirm completed tasks move below incomplete tasks in Urgency mode. Edit dates and priorities and confirm the list updates.
6. Switch sorting quickly, navigate away and back, and verify that task ordering and the selected label agree. Leave Urgency active across a due-time transition; the next refresh should update the order within roughly one minute while the screen is actively observed.
7. Review the accompanying narrative so the reflection matches your understanding and experience. Update its verification paragraph only after completing the checks above. Follow your course's assistance/disclosure policy.

The included Firebase configuration belongs to the existing project. No Firebase data was changed during preparation. Avoid using a production board for manual tests; do not publish signing keys or service-account credentials. Room migration is a separate planned database enhancement and is not claimed here.

## Complexity and tradeoffs

| Operation | Time | Additional space |
| --- | --- | --- |
| Stable merge sort | O(n log n) | O(n), plus O(log n) recursion stack |
| Heap peek | O(1) | O(1) per call |
| Heap insertion | O(log n) amortized | Heap grows to O(n) |
| Heap removal | O(log n) | O(1) per removal |
| Build by repeated insertion and drain entire heap | O(n log n) | O(n) |

These bounds treat key comparisons as constant cost; string ID/title comparisons can depend on string length. Copies and output lists retain O(n) space overall. ArrayList growth can make an individual insertion O(n), which is why its insertion bound is amortized.

The heap is rebuilt for each repository snapshot. It does not maintain incremental updates or beat merge sort asymptotically for a full displayed list. It demonstrates priority-queue operations and provides a basis for a future next-task scheduler. The custom algorithms serve the capstone's learning goals; a standard library implementation would usually lower maintenance cost in a production-only project. No measured UI speedup is claimed.
