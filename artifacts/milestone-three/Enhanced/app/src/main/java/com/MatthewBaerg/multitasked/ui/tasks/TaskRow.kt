package com.MatthewBaerg.multitasked.ui.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskRow(
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
        if (price.toDoubleOrNull() != task.price) {
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
    LaunchedEffect(task.dueDate) {
        datePickerState.selectedDateMillis = task.dueDate
    }

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "Task details expansion"
    )
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
                        textDecoration = if (task.isDone) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
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
                            isPriceError = parsedPrice == null || parsedPrice < 0.0
                            if (!isPriceError && parsedPrice != null) {
                                onPriceChange(parsedPrice)
                            }
                        },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.6f),
                        isError = isPriceError,
                        supportingText = { if (isPriceError) Text("Invalid") },
                        singleLine = true
                    )
                }

                IconButton(onClick = onExpand) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Hide details" else "Show details",
                        modifier = Modifier.rotate(rotation)
                    )
                }

                TextButton(onClick = onDelete) { Text("Delete") }
            }

            if (isExpanded) {
                TaskDetailsEditor(
                    notes = notes,
                    onNotesChange = { notes = it },
                    selectedDueDate = datePickerState.selectedDateMillis,
                    priority = priority,
                    boardMembers = boardMembers,
                    assignedUser = assignedUser,
                    onShowDatePicker = { showDatePicker = true },
                    onPriorityChange = { priority = it },
                    onAssign = onAssign,
                    onSave = {
                        onDetailsChange(notes, datePickerState.selectedDateMillis, priority)
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TaskDetailsEditor(
    notes: String,
    onNotesChange: (String) -> Unit,
    selectedDueDate: Long?,
    priority: Priority,
    boardMembers: List<User>,
    assignedUser: User?,
    onShowDatePicker: () -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onAssign: (String) -> Unit,
    onSave: () -> Unit
) {
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var userMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Due date: ")
            TextButton(onClick = onShowDatePicker) {
                Text(selectedDueDate.toDisplayDate())
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Priority: ")
            TextButton(onClick = { priorityMenuExpanded = true }) {
                Text(priority.name)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose priority")
            }
            DropdownMenu(
                expanded = priorityMenuExpanded,
                onDismissRequest = { priorityMenuExpanded = false }
            ) {
                Priority.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onPriorityChange(option)
                            priorityMenuExpanded = false
                        }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Assigned to: ")
            TextButton(onClick = { userMenuExpanded = true }) {
                Text(assignedUser?.name ?: "Unassigned")
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Assign user")
            }
            DropdownMenu(
                expanded = userMenuExpanded,
                onDismissRequest = { userMenuExpanded = false }
            ) {
                boardMembers.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.name) },
                        onClick = {
                            onAssign(member.id)
                            userMenuExpanded = false
                        }
                    )
                }
            }
        }

        Button(onClick = onSave) { Text("Save") }
    }
}

private fun Long?.toDisplayDate(): String =
    this?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
    } ?: "None"
