package com.MatthewBaerg.multitasked.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.ui.effects.ConfettiEffect
import java.text.NumberFormat

/**
 * Connects the Android lifecycle and ViewModel to the reusable screen content.
 */
@Composable
fun BoardDetailRoute(
    viewModel: BoardDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.transientErrorMessage) {
        state.transientErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(BoardDetailAction.ErrorShown)
        }
    }

    BoardDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack
    )
}

/**
 * Renders state and forwards user intent without calling repositories or starting coroutines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoardDetailScreen(
    state: BoardDetailUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (BoardDetailAction) -> Unit,
    onBack: () -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BoardDetailTopBar(
                boardName = state.board?.name ?: "Board tasks",
                isGrocery = state.board?.type == BoardType.GROCERY,
                totalCost = state.totalCost,
                selectedSort = state.sortOption,
                onBack = onBack,
                onSortSelected = { onAction(BoardDetailAction.SortChanged(it)) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NewTaskSection(
                    title = newTaskTitle,
                    onTitleChange = { newTaskTitle = it },
                    onAddTask = {
                        onAction(BoardDetailAction.AddTask(newTaskTitle))
                        newTaskTitle = ""
                    }
                )

                BoardDetailBody(
                    state = state,
                    expandedTaskId = expandedTaskId,
                    onExpandedTaskChange = { taskId ->
                        expandedTaskId = if (expandedTaskId == taskId) null else taskId
                    },
                    onAction = onAction
                )
            }

            if (state.showCelebration) {
                ConfettiEffect(
                    onFinished = { onAction(BoardDetailAction.CelebrationShown) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardDetailTopBar(
    boardName: String,
    isGrocery: Boolean,
    totalCost: Double,
    selectedSort: TaskSortOption,
    onBack: () -> Unit,
    onSortSelected: (TaskSortOption) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(boardName) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (isGrocery) {
                Text(
                    text = "Total: ${NumberFormat.getCurrencyInstance().format(totalCost)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            TextButton(onClick = { sortMenuExpanded = true }) {
                Text("Sort: ${selectedSort.displayName()}")
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false }
            ) {
                TaskSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName()) },
                        onClick = {
                            onSortSelected(option)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun NewTaskSection(
    title: String,
    onTitleChange: (String) -> Unit,
    onAddTask: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("New task") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddTask,
                    enabled = title.isNotBlank()
                ) {
                    Text("Add task")
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("New task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onAddTask,
                    enabled = title.isNotBlank()
                ) {
                    Text("Add task")
                }
            }
        }
    }
}

@Composable
private fun BoardDetailBody(
    state: BoardDetailUiState,
    expandedTaskId: String?,
    onExpandedTaskChange: (String) -> Unit,
    onAction: (BoardDetailAction) -> Unit
) {
    when {
        state.isLoading && state.tasks.isEmpty() -> LoadingState()
        state.errorMessage != null -> ErrorState(
            message = state.errorMessage,
            onRetry = { onAction(BoardDetailAction.Retry) }
        )

        else -> TaskListState(
            state = state,
            expandedTaskId = expandedTaskId,
            onExpandedTaskChange = onExpandedTaskChange,
            onAction = onAction
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun TaskListState(
    state: BoardDetailUiState,
    expandedTaskId: String?,
    onExpandedTaskChange: (String) -> Unit,
    onAction: (BoardDetailAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.totalTasks > 0) {
            TaskProgress(state.completedTasks, state.totalTasks, state.progress)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tasks yet. Add one above!")
            }
        } else {
            TaskList(
                tasks = state.tasks,
                state = state,
                expandedTaskId = expandedTaskId,
                onExpandedTaskChange = onExpandedTaskChange,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun TaskProgress(completed: Int, total: Int, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
        Text(
            text = "$completed of $total tasks completed",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TaskList(
    tasks: List<TaskItem>,
    state: BoardDetailUiState,
    expandedTaskId: String?,
    onExpandedTaskChange: (String) -> Unit,
    onAction: (BoardDetailAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                isExpanded = expandedTaskId == task.id,
                isGrocery = state.board?.type == BoardType.GROCERY,
                boardMembers = state.boardMembers,
                onToggle = { onAction(BoardDetailAction.ToggleTask(task)) },
                onPriceChange = {
                    onAction(BoardDetailAction.UpdateTaskPrice(task, it))
                },
                onDetailsChange = { notes, dueDate, priority ->
                    onAction(
                        BoardDetailAction.UpdateTaskDetails(
                            task = task,
                            notes = notes,
                            dueDate = dueDate,
                            priority = priority
                        )
                    )
                },
                onAssign = { onAction(BoardDetailAction.AssignTask(task, it)) },
                onDelete = { onAction(BoardDetailAction.DeleteTask(task)) },
                onExpand = { onExpandedTaskChange(task.id) }
            )
        }
    }
}

private fun TaskSortOption.displayName(): String =
    when (this) {
        TaskSortOption.RECENT -> "Recent"
        TaskSortOption.ALPHA_ASC -> "A to Z"
        TaskSortOption.ALPHA_DESC -> "Z to A"
        TaskSortOption.COMPLETED_FIRST -> "Completed first"
        TaskSortOption.INCOMPLETE_FIRST -> "Incomplete first"
    }
