# MultiTasked Milestone Two

## Enhancement scope

This version strengthens the existing MVVM and Jetpack Compose design around the board-detail feature. It does not implement the priority-queue, custom sorting algorithm, coroutine-dispatcher, or Room enhancements reserved for later milestones.

## What changed

1. `BoardDetailContract.kt` defines one immutable UI state and a sealed set of user actions.
2. `BoardDetailScreen.kt` separates lifecycle/ViewModel collection in `BoardDetailRoute` from reusable screen rendering. It also divides loading, error, empty, progress, and task-list content into focused composables.
3. `TaskRow.kt` moves the task editor out of the screen file and keeps row-specific temporary UI state near the row.
4. `BoardDetailViewModel.kt` accepts typed actions, validates task titles and prices, centralizes coroutine error handling, preserves cancellation, and reports safe user messages.
5. `CelebrationSettings` narrows the ViewModel's dependency and makes the feature testable without Android `Context` or DataStore.
6. `TaskSortPolicy.kt` moves sort selection out of the ViewModel and fixes the existing Recent option to use `createdAt` rather than the Firestore document ID.
7. The duplicate celebration renderer was reduced to one overlay. Celebration begins only after a successful repository update.
8. The back button now uses the `IconButton` click handler directly, giving the full control the expected touch target and semantics.
9. The add-task controls use a compact vertical layout on phones and a horizontal layout at widths of 600 dp or greater.
10. The unreferenced duplicate `com.baerg...UserRepository` file was removed after a project-wide reference search found no usages.
11. The placeholder arithmetic test was replaced with tests for summary values, sorting, ViewModel validation, input normalization, and safe failure behavior.

## Files to review in order

1. `app/src/main/java/com/MatthewBaerg/multitasked/ui/tasks/BoardDetailContract.kt`
2. `app/src/main/java/com/MatthewBaerg/multitasked/ui/tasks/BoardDetailScreen.kt`
3. `app/src/main/java/com/MatthewBaerg/multitasked/ui/tasks/TaskRow.kt`
4. `app/src/main/java/com/MatthewBaerg/multitasked/ui/tasks/BoardDetailViewModel.kt`
5. `app/src/main/java/com/MatthewBaerg/multitasked/ui/tasks/TaskSortPolicy.kt`
6. `app/src/main/java/com/MatthewBaerg/multitasked/ui/Settings.kt`
7. `app/src/main/java/com/MatthewBaerg/multitasked/di/AppModule.kt`
8. `app/src/test/java/com/MatthewBaerg/multitasked/ui/tasks/`

## Verification in Android Studio

1. Open the enhanced project and allow Gradle sync to finish.
2. Select **Build > Make Project**.
3. Run the local tests with `./gradlew testDebugUnitTest` or the Android Studio test gutter.
4. Run the app and verify sign in, board opening, task addition, sorting, completion, editing, assignment, deletion, error Snackbar behavior, celebration, and back navigation.
5. Record the test result and emulator/device configuration in the narrative before final submission if the instructor expects execution evidence.

## Expected local test coverage

- Task summary calculations and empty-list progress
- Recent ordering by creation timestamp
- Deterministic incomplete-first ordering
- Task-title trimming before repository calls
- Rejection of negative prices before repository calls
- Generic user-facing errors and error-type-only logging
