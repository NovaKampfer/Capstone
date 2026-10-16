# Database enhancement study guide

All Kotlin paths below start at `Enhanced/app/src/main/java/com/MatthewBaerg/multitasked/`. Compare against `Milestone_Three_Baseline` before studying each change. The original database files existed but contained zero bytes.

## File order

| Order | File | What to explain |
| ---: | --- | --- |
| 1 | data/local/BoardEntity.kt | Primary key and the separate summary projection |
| 2 | data/local/TaskEntity.kt | Foreign key, cascade, composite index, nullable date and defaults |
| 3 | data/local/BoardDao.kt | JOIN, COUNT, bound values, ABORT inserts and transaction |
| 4 | data/local/TaskDao.kt | Read, update and delete scoped by board and task IDs |
| 5 | data/local/AppDatabase.kt | Version, migration and database builder |
| 6 | data/local/Mappers.kt | Conversion to the existing task model |
| 7 | data/local/LocalBoardRepository.kt | Validation and coordination of DAO calls |
| 8 | di/DatabaseModule.kt | One shared database instance through Hilt |
| 9 | ui/local/LocalDateInput.kt | Strict dates and UTC timestamp conversion |
| 10 | ui/local/LocalWorkspaceViewModel.kt | Observable state, writes, errors and cancellation |
| 11 | ui/local/LocalWorkspaceScreen.kt | CRUD forms, confirmation and sorting |
| 12 | MainActivity.kt | Local versus shared workspace selection |
| 13 | app/build.gradle.kts and res/xml backup files | Dependencies, schema export and backup exclusions |
| 14 | app/src/androidTest/.../LocalDatabaseTest.kt | Actual Room integration tests to run locally |

## Read the entities line by line

`@Entity` associates a Kotlin class with a table. `@PrimaryKey` makes each UUID unique. A task stores a board ID rather than duplicating the board's name. That gives the database a one-to-many relationship: one board can have many tasks.

`ForeignKey.CASCADE` makes a board deletion remove its child rows atomically. An orphan task with no parent is rejected. This protects relational integrity; it is not an account authorization mechanism. The local workspace is intentionally device-scoped.

The composite index begins with boardId because queries first restrict rows to a board. createdAt supports the next ordering key. Indexes speed selected reads at the cost of storage and write work. The ID tie-break is outside this index, so do not claim the complete ORDER BY is always covered.

Defaults on notes and priority match the migration SQL. dueDate is nullable, so an undated task does not need a fake timestamp. The priority string is produced from the existing Priority enum. Unexpected stored values fail mapping instead of silently inventing a priority.

Explain: Why would a duplicate task counter create consistency work? Why is a foreign key stronger than only checking a board ID in the UI?

## Follow CRUD through the DAO

The annotation above each function defines SQL. Room generates the implementation during the Android build and verifies the queries. `:id`, `:boardId`, and the other named parameters are bound values; user input is not pasted into an SQL command.

The board list starts with a LEFT JOIN so a board with zero tasks still appears. COUNT(t.id) returns zero for that board. GROUP BY collects each board's task rows into one result. BoardSummary contains only the fields needed by the list.

`suspend` insert/update/delete operations can be called from a coroutine. Room performs its asynchronous database work on its managed executors. A Flow query emits data as observed tables change, so the application does not have to manually refresh its list after every edit.

`createWithTasks` is a transaction: if an optional first task fails, the parent insert rolls back too. ABORT prevents a duplicate board ID from replacing a parent record and cascading its children away. The normal deleteBoard statement is already atomic; cascade handles its dependent rows.

Task updates include both IDs in WHERE. If a task is deleted or belongs to another board, the affected-row count is zero. The repository checks that count and reports a missing record rather than pretending the edit succeeded.

Explain: Why should the editor close only after a successful write? What changes if REPLACE is used instead of ABORT on a parent table?

## Understand schema evolution

AppDatabase registers two entities and version 2. Its migration from version 1 adds notes, dueDate, and priority without dropping tables. Existing rows receive an empty note, a null date, and NONE priority. The code avoids fallbackToDestructiveMigration because silently rebuilding tables would lose local data.

The earlier project had no live Room database. Version 1 is an explicit fixture for demonstrating schema evolution; this is not a claim that existing Firebase users have been migrated. The instrumentation test creates that v1 fixture, opens it through Room, then reads and updates the preserved row. Room's generated validation must pass during that opening.

`exportSchema = true` and the kapt schemaLocation argument request a compiler-generated JSON description. The export still needs the successful Android build. Do not rename the SQL fixture to JSON or label it as a generated schema.

Explain: Why do non-null columns need defaults when old rows already exist? Why is a Room version migration different from importing a Firestore collection?

## Follow one edit from screen to disk

The screen collects ViewModel state with lifecycle awareness. Selecting Save sends the entered title, notes, date, and priority to the ViewModel. The ViewModel prevents duplicate concurrent writes, parses the date, and calls the repository in viewModelScope.

The repository trims and validates text, then calls the appropriate DAO method. New tasks receive UUIDs and timestamps. Existing tasks retain their IDs, creation time and completion flag when their details change. The SQL update affects only the chosen row. The observed Flow emits a refreshed list, which passes through the Milestone Three sorting service before returning to the screen.

CancellationException is rethrown so canceled work can stop normally. Other storage failures show a safe message. The form remains available after a failed write. The database is provided once per application by Hilt, using the application Context so it does not retain an Activity.

Dates are interpreted as midnight UTC. This keeps conversion deterministic, but a date can become overdue before the user's local calendar day ends. A future product decision could model end-of-day deadlines in the user's time zone; it should be tested as a change to the ordering rules.

## Security and privacy limits

Parameterized SQL handles text such as apostrophes safely. Validation also constrains empty or excessively long inputs. Android app-private storage and database backup exclusions reduce unintended exposure. Local records are accessible to anyone who can open this installation. They are not encrypted by Room or isolated by Firebase user. Device compromise, encryption, per-user local separation, and export/recovery need additional design if the app stores sensitive data.

## Test before presenting

Run the Android commands in README. The ten instrumentation tests use temporary databases and the real Room implementation. Inspect each test's setup, operation, and expected result. The supplemental 11 SQLite checks and 43 JVM tests passed here; they do not replace code generation or emulator checks.

Practice explaining: one JOIN result, a failed transaction, a cascade deletion, how an edit reaches the database, and how the v1 migration preserves a saved task. Review the narrative so its learning statements reflect your actual understanding.
