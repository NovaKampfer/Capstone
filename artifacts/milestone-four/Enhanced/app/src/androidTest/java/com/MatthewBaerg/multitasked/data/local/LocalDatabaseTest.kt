package com.MatthewBaerg.multitasked.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.MatthewBaerg.multitasked.data.model.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalDatabaseTest {
    private lateinit var db: AppDatabase
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Before fun open() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun close() { db.close() }
    private fun board(id: String = "b") = BoardEntity(id, "Board", 1)
    private fun task(id: String = "t", boardId: String = "b") = TaskEntity(id, boardId, "Task", false, 2)

    @Test fun boardAndTaskCrudAndCounts() = runBlocking {
        db.boardDao().createWithTasks(board(), listOf(task()))
        assertEquals(1L, db.boardDao().observeBoards().first().single().taskCount)
        assertEquals(1, db.boardDao().rename("b", "Renamed"))
        assertEquals("Renamed", db.boardDao().observeBoard("b").first()!!.name)
        assertEquals(1, db.taskDao().updateDetails("b", "t", "Edited", "Note", 123L, "HIGH"))
        assertEquals(1, db.taskDao().setCompleted("b", "t", true))
        val row = db.taskDao().get("b", "t")!!
        assertEquals("Edited", row.title)
        assertEquals("Note", row.notes)
        assertEquals(123L, row.dueDate)
        assertEquals("HIGH", row.priority)
        assertTrue(row.completed)
        assertEquals(1, db.taskDao().delete("b", "t"))
        assertEquals(0L, db.boardDao().observeBoards().first().single().taskCount)
        assertEquals(1, db.boardDao().delete("b"))
        assertTrue(db.boardDao().observeBoards().first().isEmpty())
    }
    @Test fun wrongBoardCannotReadOrChangeTask() = runBlocking {
        db.boardDao().createWithTasks(board(), listOf(task()))
        db.boardDao().insert(board("other"))
        assertNull(db.taskDao().get("other", "t"))
        assertEquals(0, db.taskDao().delete("other", "t"))
        assertEquals(0, db.taskDao().setCompleted("other", "t", true))
        assertEquals(0, db.taskDao().updateDetails("other", "t", "x", "", null, "NONE"))
        assertEquals("Task", db.taskDao().get("b", "t")!!.title)
    }
    @Test fun deletingBoardCascadesOnlyItsTasks() = runBlocking {
        db.boardDao().createWithTasks(board(), listOf(task()))
        db.boardDao().createWithTasks(board("other"), listOf(task("other-task", "other")))
        db.boardDao().delete("b")
        assertTrue(db.taskDao().observeTasks("b").first().isEmpty())
        assertNotNull(db.taskDao().get("other", "other-task"))
    }
    @Test fun orphanTaskIsRejected() = runBlocking {
        var failed = false
        try { db.taskDao().insert(task()) } catch (_: android.database.sqlite.SQLiteConstraintException) { failed = true }
        assertTrue(failed)
    }
    @Test fun duplicateBoardDoesNotReplaceOrDeleteChildren() = runBlocking {
        db.boardDao().createWithTasks(board(), listOf(task()))
        var failed = false
        try { db.boardDao().insert(board().copy(name = "Replacement")) }
        catch (_: android.database.sqlite.SQLiteConstraintException) { failed = true }
        assertTrue(failed)
        assertEquals("Board", db.boardDao().observeBoard("b").first()!!.name)
        assertNotNull(db.taskDao().get("b", "t"))
    }
    @Test fun failedMultiInsertRollsBackBoardAndTasks() = runBlocking {
        var failed = false
        try { db.boardDao().createWithTasks(board(), listOf(task(), task())) }
        catch (_: android.database.sqlite.SQLiteConstraintException) { failed = true }
        assertTrue(failed)
        assertNull(db.boardDao().observeBoard("b").first())
        assertTrue(db.taskDao().observeTasks("b").first().isEmpty())
    }
    @Test fun sqlLookingInputIsStoredAsText() = runBlocking {
        val text = "Robert'); DROP TABLE local_tasks;--"
        db.boardDao().createWithTasks(board(), listOf(task().copy(title = text)))
        assertEquals(text, db.taskDao().get("b", "t")!!.title)
        assertEquals(1, db.taskDao().observeTasks("b").first().size)
    }
    @Test fun repositoryRejectsInvalidInputWithoutWriting() = runBlocking {
        val repository = LocalBoardRepository(db)
        var failed = false
        try { repository.createBoard("   ") } catch (_: IllegalArgumentException) { failed = true }
        assertTrue(failed)
        assertTrue(repository.observeBoards().first().isEmpty())
        repository.createBoard(" Valid ", " First ")
        val id = repository.observeBoards().first().single().id
        assertEquals("Valid", repository.observeBoard(id).first()!!.name)
        failed = false
        try { repository.saveTask(id, null, "Task", "x".repeat(2001), null, Priority.NONE) }
        catch (_: IllegalArgumentException) { failed = true }
        assertTrue(failed)
        assertEquals(1, repository.observeTasks(id).first().size)
    }
    @Test fun fileDatabaseSurvivesCloseAndReopen() = runBlocking {
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        try {
            val first = AppDatabase.create(context, name)
            try { first.boardDao().createWithTasks(board(), listOf(task())) } finally { first.close() }
            val second = AppDatabase.create(context, name)
            try { assertEquals("Task", second.taskDao().get("b", "t")!!.title) } finally { second.close() }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun migrationPreservesVersionOneRowsAndAddsDefaults() = runBlocking {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { legacy ->
                val sql = InstrumentationRegistry.getInstrumentation().context.assets
                    .open("schema-v1.sql").bufferedReader().use { it.readText() }
                sql.split(';').filter { it.isNotBlank() }.forEach { legacy.execSQL(it) }
                legacy.execSQL("INSERT INTO local_boards VALUES ('b', 'Original', 1)")
                legacy.execSQL("INSERT INTO local_tasks VALUES ('t', 'b', 'Keep me', 1, 2)")
                legacy.version = 1
            }
            val upgraded = AppDatabase.create(context, name)
            try {
                // Room executes the migration AND generated schema validation when opened.
                val row = upgraded.taskDao().get("b", "t")!!
                assertEquals("Keep me", row.title)
                assertTrue(row.completed)
                assertEquals("", row.notes)
                assertNull(row.dueDate)
                assertEquals("NONE", row.priority)
                assertEquals(1, upgraded.taskDao().updateDetails("b", "t", "Updated", "Note", 55L, "HIGH"))
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
    }
}
