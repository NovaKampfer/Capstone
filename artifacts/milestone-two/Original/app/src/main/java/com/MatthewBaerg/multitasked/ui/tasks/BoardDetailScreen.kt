package com.MatthewBaerg.multitasked.ui.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User
import com.MatthewBaerg.multitasked.ui.effects.ConfettiEffect
import com.MatthewBaerg.multitasked.util.debounceClick
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    viewModel: BoardDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var newTaskTitle by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.transientErrorMessage) {
        state.transientErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorShown()
        }
    }

    if (state.showCelebration) {
        ConfettiEffect(onFinished = viewModel::onCelebrationShown)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.board?.name ?: "Board tasks") },
                navigationIcon = {
                    IconButton(onClick = { /* Debounce click */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.debounceClick(onBack)
                        )
                    }
                },
                actions = {
                    if (state.board?.type == BoardType.GROCERY) {
                        Text(
                            text = "Total: ${NumberFormat.getCurrencyInstance().format(state.totalCost)}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text("Sort")
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Recent") }, onClick = { viewModel.setSort(TaskSortOption.RECENT); sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("A → Z") }, onClick = { viewModel.setSort(TaskSortOption.ALPHA_ASC); sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Z → A") }, onClick = { viewModel.setSort(TaskSortOption.ALPHA_DESC); sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Completed first") }, onClick = { viewModel.setSort(TaskSortOption.COMPLETED_FIRST); sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Incomplete first") }, onClick = { viewModel.setSort(TaskSortOption.INCOMPLETE_FIRST); sortMenuExpanded = false })
                    }
                }
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
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("New task") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.addTask(newTaskTitle.trim())
                            newTaskTitle = ""
                        }
                    },
                    enabled = newTaskTitle.isNotBlank()
                ) {
                    Text("Add task")
                }

                if (state.isLoading && state.tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.errorMessage != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.errorMessage ?: "An unknown error occurred",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.onRetry() }) { Text("Retry") }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.totalTasks > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                )
                                Text(
                                    text = "${state.completedTasks} of ${state.totalTasks} tasks completed",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.tasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tasks yet. Add one above!")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.tasks, key = { it.id }) { task ->
                                    TaskRow(
                                        task = task,
                                        isExpanded = expandedTaskId == task.id,
                                        isGrocery = state.board?.type == BoardType.GROCERY,
                                        boardMembers = state.boardMembers,
                                        onToggle = { viewModel.toggleTaskDone(task) },
                                        onPriceChange = { price -> viewModel.updateTaskPrice(task, price) },
                                        onDetailsChange = { notes, dueDate, priority -> viewModel.updateTaskDetails(task, notes, dueDate, priority) },
                                        onAssign = { userId -> viewModel.assignTask(task, userId) },
                                        onDelete = { viewModel.deleteTask(task) },
                                        onExpand = { expandedTaskId = if (expandedTaskId == task.id) null else task.id }
                                    )
                                 }
                            }
                        }
                    }
                }
            }

            if (state.showCelebration) {
                ConfettiEffect(onFinished = viewModel::onCelebrationShown)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskItem,
    isExpanded: Boolean,
    isGrocery: Boolean,
    boardMembers: List<User>,
    onToggle: () -> Unit,
    onPriceChange: (Double) -> Unit,
    onDetailsChange: (String, Long?, Priority) -> Unit,
    onAssign: (String) -> Unit,
    onDelete: () -> Unit,
    onExpand: () -> Unit
) {
    var notes by remember { mutableStateOf(task.notes) }
    var price by remember { mutableStateOf(task.price.toString()) }
    var isPriceError by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(task.priority) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(task.price) {
        val currentPrice = price.toDoubleOrNull()
        if (currentPrice != task.price) {
            price = task.price.toString()
        }
    }
    LaunchedEffect(task.notes) {
        if (notes != task.notes) {
            notes = task.notes
        }
    }
    LaunchedEffect(task.priority) {
        if (priority != task.priority) {
            priority = task.priority
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = task.dueDate)
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "")
    val assignedUser = boardMembers.find { it.id == task.assignedTo }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (assignedUser != null) {
                        Text(
                            text = "Assigned to: ${assignedUser.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (isGrocery) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { newPrice ->
                            price = newPrice
                            val parsedPrice = newPrice.toDoubleOrNull()
                            isPriceError = parsedPrice == null
                            if (parsedPrice != null) {
                                onPriceChange(parsedPrice)
                            }
                        },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.6f),
                        isError = isPriceError,
                        supportingText = { if (isPriceError) Text("Invalid") }
                    )
                }

                IconButton(onClick = onExpand) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "More info",
                        modifier = Modifier.rotate(rotation)
                    )
                }

                TextButton(onClick = onDelete) { Text("Delete") }
            }

            if (isExpanded) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Due Date: ")
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(task.dueDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "None")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Priority: ")
                        var priorityMenuExpanded by remember { mutableStateOf(false) }
                        TextButton(onClick = { priorityMenuExpanded = true }) {
                            Text(priority.name)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Priority")
                        }
                        DropdownMenu(expanded = priorityMenuExpanded, onDismissRequest = { priorityMenuExpanded = false }) {
                            Priority.values().forEach { prio ->
                                DropdownMenuItem(text = { Text(prio.name) }, onClick = { priority = prio; priorityMenuExpanded = false })
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Assigned To: ")
                        var userMenuExpanded by remember { mutableStateOf(false) }
                        TextButton(onClick = { userMenuExpanded = true }) {
                            Text(assignedUser?.name ?: "Unassigned")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Assign user")
                        }
                        DropdownMenu(expanded = userMenuExpanded, onDismissRequest = { userMenuExpanded = false }) {
                            boardMembers.forEach { member ->
                                DropdownMenuItem(text = { Text(member.name) }, onClick = { onAssign(member.id); userMenuExpanded = false })
                            }
                        }
                    }

                    Button(onClick = { onDetailsChange(notes, datePickerState.selectedDateMillis, priority) }) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
