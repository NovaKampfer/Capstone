# Milestone Three enhancement

This is the current enhanced project. Open this directory in Android Studio.

The package-level `README.md`, `LEARNING_GUIDE.md`, and `Evidence/VALIDATION.md` document the new algorithms, changes, and verification limits. `MILESTONE_TWO_README.md` is retained as historical documentation; it does not describe the new sorting features.

New options in the task sorting menu are **Due date** and **Urgency**. Both use custom algorithms selected by `TaskSortPolicy` and run through `TaskSortingService` on an injected background dispatcher. The existing Firebase data layer is unchanged.

Run `./gradlew assembleDebug testDebugUnitTest` on a configured Android development machine (Windows: `.\gradlew.bat assembleDebug testDebugUnitTest`). The complete Android build has not been verified in the preparation environment. The standalone algorithm/coroutine test results are included at the package root.
