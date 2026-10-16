# MultiTasked — Milestone Four database enhancement

Matthew Baerg • CS 499 • October 6, 2026

This submission implements Room-backed local boards and tasks with Create, Read, Update, and Delete operations. Launch the enhanced application and choose **On this device**. The new workspace requires no account and uses persisted records rather than a hardcoded task list.

## Contents

| Folder or file | Purpose |
| --- | --- |
| Original/ | Original application from the earlier submission, unchanged |
| Milestone_Three_Baseline/ | Immediate pre-enhancement project, unchanged |
| Enhanced/ | Current Android project, including Milestone Two and Three enhancements |
| DATABASE_WALKTHROUGH.md | File order, explanations, schema, and study questions |
| Evidence/ | Actual test outputs, build-attempt log, validation scope, and source hashes |
| verification/ | Repeatable supplemental SQLite and JVM checks |

The accompanying Word narrative is submitted separately. Prior README files inside historical projects refer to their original milestones.

## Scope and behavior

The baseline already used Firebase for shared boards, and six local database files were empty. This enhancement fills those files and adds a device workspace. It does not import or synchronize existing Firestore records. The **Shared boards** choice retains the earlier cloud functionality. If your approved plan requires replacing Firebase completely, that scope difference needs instructor discussion; this implementation demonstrates the planned local Room CRUD capability.

The local workspace supports creating a board with an optional first task, renaming boards, deleting boards with confirmation, adding tasks, reading saved lists, editing titles/notes/dates/priorities, changing completion status, and deleting tasks with confirmation. It reuses the Milestone Three background sorting service for Recent, Due date, and Urgency ordering.

Room stores data in the app-private `multitasked-local.db` file. Local data is device-scoped, not assigned to a Firebase account. Anyone using this app installation can access it, including after switching cloud accounts. The screen states this explicitly. Android backup and device-transfer rules exclude database files. Room does not add database encryption. Uninstalling the app or clearing its storage removes these local records; no local export or cloud synchronization is implemented.

## Database design

- Two related tables: `local_boards` and `local_tasks`.
- UUID string primary keys, a required task-to-board foreign key, and cascade deletion.
- A composite index on `(boardId, createdAt)` supports board-scoped task access. The final ID tie-break may still require an extra sort operation.
- Board task counts are derived with a LEFT JOIN and COUNT; there is no duplicate counter to keep synchronized.
- DAO statements use bound parameters. Updates and deletes check both board ID and task ID.
- Board creation and an optional first task use one Room transaction. Inserts use ABORT rather than REPLACE, avoiding accidental cascade deletion on duplicate parent IDs.
- Repository validation limits names/titles to 1–120 trimmed characters and notes to 2,000 characters. Date input is strict YYYY-MM-DD and stores midnight UTC; clearing the field stores NULL.
- Coroutine DAO methods and Flow queries keep database work asynchronous. The UI reports write errors and closes edit dialogs only after successful writes.
- Schema version 2 includes a non-destructive 1-to-2 migration adding notes, due date, and priority. Version 1 is a documented test schema; the earlier application did not have an active Room database to migrate. This is not a Firestore migration.

Room 2.6.1 is pinned for compatibility with the existing Kotlin 2.0.21/kapt project. It is a deliberate compatibility choice, not a claim to use the latest Room major version. Modernizing the annotation-processing toolchain is a separate change.

## Validation completed here

- **43 JUnit tests passed**: the 38 existing algorithm/service/state tests plus five new date-input tests.
- The local entities, DAOs, database class, mapper, and repository **type-check** with Kotlin 2.0.21 against official Room/API jars. This did not execute Room annotation processing.
- **11 SQLite checks passed**, using the actual query strings and migration statements extracted from the Kotlin source. They cover CRUD, scope, constraints, cascade behavior, rollback, counts, literal parameter handling, index use, migration preservation, and persistence after reopening a file.
- The original and Milestone Three baseline files are unchanged.

**Still required:** full Android compilation, Room/Hilt generated-code validation, six existing ViewModel tests, ten new Room instrumentation tests, and a device/emulator walkthrough. The Gradle wrapper could not download its distribution in this environment; an Android SDK is also unavailable. Supplemental tests do not replace Android validation. No APK or successful full-build result is claimed.

## Final Android Studio verification

1. Extract the ZIP and open **Enhanced** in Android Studio. Keep its existing Gradle wrapper, Kotlin, and Android plugin versions. Use a compatible Gradle JDK (17+) and install Android SDK 36.
2. In the Enhanced terminal on Windows, run:

   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest
   ```

3. Start an emulator or connect a test device, then run the Room suite:

   ```powershell
   .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.MatthewBaerg.multitasked.data.local.LocalDatabaseTest"
   ```

4. Review `app/build/reports/tests/testDebugUnitTest/index.html` and the connected-test report under `app/build/reports/androidTests/connected/`. There are 49 local JVM test methods total and 10 Room instrumentation methods; verify actual discovered tests and failures rather than relying on counts alone.
5. On the device, choose **On this device**, create a board and first task, edit the task, set a due date and priority, complete it, and try the three sort options. Close and relaunch the application to check persistence. Repeat with the network disabled.
6. Delete one task and then a board with tasks. Confirm another board remains intact. Check a blank title and invalid calendar date are rejected without discarding the editor's current values.
7. Inspect the generated schema JSON in `app/schemas/` and include it when repackaging after successful validation. Review the narrative against your own learning and update its validation statement only with results you actually obtained. Keep test data free of real personal information.

The database suite uses temporary/in-memory databases; it does not read or change the production local file or Firebase data. AI assistance was used to prepare the enhancement and documentation. The walkthrough is included so you can study and explain each change.
