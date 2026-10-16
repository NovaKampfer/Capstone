package com.MatthewBaerg.multitasked.ui.local

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.MatthewBaerg.multitasked.data.local.BoardSummary
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.ui.tasks.TaskSortOption

@Composable
fun LocalWorkspaceRoute(onExit: () -> Unit, viewModel: LocalWorkspaceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = state.selection
    var newBoard by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<BoardSummary?>(null) }
    var deleteBoard by remember { mutableStateOf<BoardSummary?>(null) }
    var editTask by remember { mutableStateOf<TaskItem?>(null) }
    var newTask by remember { mutableStateOf(false) }
    var deleteTask by remember { mutableStateOf<TaskItem?>(null) }
    val goBack: () -> Unit = { if (selected.id != null) viewModel.selectBoard(null) else onExit() }
    BackHandler { if (!state.busy) goBack() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = goBack, enabled = !state.busy) { Text("Back") }
        Text(selected.board?.name ?: "On this device", style = MaterialTheme.typography.headlineMedium)
        Text("Stored on this device only. Available to anyone using this app installation.",
            style = MaterialTheme.typography.bodySmall)
        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::retryRead, enabled = !state.busy) { Text("Retry reading data") }
        }
        if (state.loading) CircularProgressIndicator()
        if (selected.id == null) {
            Button(onClick = { newBoard = true }, enabled = !state.busy) { Text("Create board") }
            if (!state.loading && state.boards.isEmpty()) Text("No local boards yet. Create your first board.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.boards, key = { it.id }) { board ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            TextButton(onClick = { viewModel.selectBoard(board.id) }, enabled = !state.busy) {
                                Text("${board.name} (${board.taskCount} tasks)")
                            }
                            Row {
                                TextButton(onClick = { rename = board }, enabled = !state.busy) { Text("Rename") }
                                TextButton(onClick = { deleteBoard = board }, enabled = !state.busy) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        } else if (selected.board == null) {
            Text("This board is no longer available. Return to the board list.")
        } else {
            Row {
                Button(onClick = { newTask = true }, enabled = !state.busy) { Text("Add task") }
                SortMenu(selected.option, viewModel::sortBy)
            }
            if (selected.tasks.isEmpty()) Text("No tasks yet.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selected.tasks, key = { it.id }) { task ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row {
                                Checkbox(checked = task.isDone, enabled = !state.busy,
                                    onCheckedChange = { viewModel.complete(task, it) })
                                Text(task.title, modifier = Modifier.weight(1f).padding(top = 12.dp))
                            }
                            Text("Priority: ${task.priority.name.lowercase()} | Due: ${LocalDateInput.format(task.dueDate).ifBlank { "none" }}")
                            if (task.notes.isNotBlank()) Text(task.notes)
                            Row {
                                TextButton(onClick = { editTask = task }, enabled = !state.busy) { Text("Edit") }
                                TextButton(onClick = { deleteTask = task }, enabled = !state.busy) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (newBoard || rename != null) {
        val board = rename
        BoardEditor(board?.name ?: "", board == null, state.busy, state.error,
            dismiss = { newBoard = false; rename = null },
            save = { name, first ->
                val close = { newBoard = false; rename = null }
                if (board == null) viewModel.createBoard(name, first, close)
                else viewModel.renameBoard(board.id, name, close)
            })
    }
    if (newTask || editTask != null) {
        val task = editTask
        TaskEditor(task, state.busy, state.error, dismiss = { newTask = false; editTask = null }) { title, notes, date, priority ->
            selected.id?.let { id ->
                viewModel.saveTask(id, task?.id, title, notes, date, priority) { newTask = false; editTask = null }
            }
        }
    }
    deleteBoard?.let { board ->
        DeleteConfirmation("Delete ${board.name} and all its tasks?", state.busy, state.error,
            { deleteBoard = null }, { viewModel.deleteBoard(board.id) { deleteBoard = null } })
    }
    deleteTask?.let { task ->
        DeleteConfirmation("Delete ${task.title}?", state.busy, state.error,
            { deleteTask = null }, { viewModel.deleteTask(task) { deleteTask = null } })
    }
}

@Composable
private fun SortMenu(option: TaskSortOption, select: (TaskSortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    fun label(value: TaskSortOption) = when (value) {
        TaskSortOption.DUE_DATE -> "Due date"
        TaskSortOption.URGENCY -> "Urgency"
        else -> "Recent"
    }
    Box {
        TextButton(onClick = { expanded = true }) { Text("Sort: ${label(option)}") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            listOf(TaskSortOption.RECENT, TaskSortOption.DUE_DATE, TaskSortOption.URGENCY).forEach { value ->
                DropdownMenuItem(text = { Text(label(value)) }, onClick = { select(value); expanded = false })
            }
        }
    }
}

@Composable
private fun BoardEditor(initial: String, creating: Boolean, busy: Boolean, error: String?,
                        dismiss: () -> Unit, save: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    var first by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() },
        title = { Text(if (creating) "Create board" else "Rename board") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Board name") }, enabled = !busy)
                if (creating) OutlinedTextField(first, { first = it }, label = { Text("First task (optional)") }, enabled = !busy)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { save(name, first) }, enabled = !busy && name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } })
}

@Composable
private fun TaskEditor(task: TaskItem?, busy: Boolean, error: String?, dismiss: () -> Unit,
                       save: (String, String, String, Priority) -> Unit) {
    var title by rememberSaveable { mutableStateOf(task?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(task?.notes ?: "") }
    var date by rememberSaveable { mutableStateOf(LocalDateInput.format(task?.dueDate)) }
    var priority by rememberSaveable { mutableStateOf(task?.priority ?: Priority.NONE) }
    var prioritiesOpen by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() },
        title = { Text(if (task == null) "Add task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, enabled = !busy)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, enabled = !busy)
                OutlinedTextField(date, { date = it }, label = { Text("Due date (YYYY-MM-DD, UTC)") }, enabled = !busy)
                Text("Leave date empty for no deadline.", style = MaterialTheme.typography.bodySmall)
                Box {
                    TextButton(onClick = { prioritiesOpen = true }, enabled = !busy) { Text("Priority: ${priority.name.lowercase()}") }
                    DropdownMenu(prioritiesOpen, onDismissRequest = { prioritiesOpen = false }) {
                        Priority.entries.forEach { value ->
                            DropdownMenuItem(text = { Text(value.name.lowercase()) }, onClick = { priority = value; prioritiesOpen = false })
                        }
                    }
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { save(title, notes, date, priority) }, enabled = !busy && title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } })
}

@Composable
private fun DeleteConfirmation(message: String, busy: Boolean, error: String?, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text("Confirm deletion") },
        text = { Column { Text(message); if (error != null) Text(error, color = MaterialTheme.colorScheme.error) } },
        confirmButton = { TextButton(onClick = confirm, enabled = !busy) { Text("Delete") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } })
}
