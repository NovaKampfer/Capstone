package com.MatthewBaerg.multitasked.ui.boards

import android.content.Intent
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.ui.theme.AppTheme
import com.MatthewBaerg.multitasked.util.debounceClick

private sealed class ActiveDialog {
    object Create : ActiveDialog()
    object Join : ActiveDialog()
    object Settings : ActiveDialog()
    object Profile : ActiveDialog()
    data class Delete(val board: Board) : ActiveDialog()
    data class Edit(val board: Board) : ActiveDialog()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsScreen(
    viewModel: BoardsViewModel,
    onBoardSelected: (String) -> Unit,
    onLogout: () -> Unit,
    onProfile: () -> Unit,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.transientErrorMessage) {
        state.transientErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Your Boards") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { activeDialog = ActiveDialog.Create }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Create board")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* already here */ },
                    label = { Text("Boards") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { activeDialog = ActiveDialog.Join },
                    label = { Text("Join") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { activeDialog = ActiveDialog.Settings },
                    label = { Text("Settings") },
                    icon = {}
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("Search boards") },
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showSortMenu = true }) {
                    Text("Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { viewModel.setSortOption(BoardSortOption.NAME_ASC); showSortMenu = false })
                    DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { viewModel.setSortOption(BoardSortOption.NAME_DESC); showSortMenu = false })
                    DropdownMenuItem(text = { Text("Task count (most)") }, onClick = { viewModel.setSortOption(BoardSortOption.TASK_COUNT_DESC); showSortMenu = false })
                    DropdownMenuItem(text = { Text("Task count (least)") }, onClick = { viewModel.setSortOption(BoardSortOption.TASK_COUNT_ASC); showSortMenu = false })
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "An unknown error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (state.filteredBoards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No boards found. Try a different search or create one!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredBoards, key = { it.id }) { board ->
                        BoardCard(
                            board = board,
                            onClick = { onBoardSelected(board.id) },
                            onShareClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Join my MultiTasked board! ID: ${board.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            onEditClick = { activeDialog = ActiveDialog.Edit(board) },
                            onDeleteClick = { activeDialog = ActiveDialog.Delete(board) }
                        )
                    }
                }
            }
        }
    }

    when (val dialog = activeDialog) {
        is ActiveDialog.Create -> {
            CreateBoardDialog(
                onDismiss = { activeDialog = null },
                onCreate = { name, desc, type ->
                    viewModel.createBoard(name, desc, type)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.Join -> {
            JoinBoardDialog(
                onDismiss = { activeDialog = null },
                onJoin = { boardId ->
                    viewModel.joinBoard(boardId)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.Settings -> {
            SettingsDialog(
                onDismiss = { activeDialog = null },
                onProfileClick = { onProfile() },
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                showCelebration = state.showCelebration,
                onShowCelebrationChange = viewModel::setShowCelebration
            )
        }
        is ActiveDialog.Profile -> {
            onProfile()
            activeDialog = null
        }
        is ActiveDialog.Delete -> {
            DeleteBoardDialog(
                boardName = dialog.board.name,
                onDismiss = { activeDialog = null },
                onConfirm = {
                    viewModel.deleteBoard(dialog.board.id)
                    activeDialog = null
                }
            )
        }
        is ActiveDialog.Edit -> {
            EditBoardDialog(
                board = dialog.board,
                onDismiss = { activeDialog = null },
                onConfirm = { name, desc ->
                    viewModel.updateBoard(dialog.board.id, name, desc)
                    activeDialog = null
                }
            )
        }
        null -> Unit // No dialog to show
    }
}

@Composable
private fun EditBoardDialog(board: Board, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf(board.name) }
    var description by remember { mutableStateOf(board.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Board") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteBoardDialog(boardName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Board") },
        text = { Text("Are you sure you want to delete the board '$boardName'? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateBoardDialog(onDismiss: () -> Unit, onCreate: (String, String, BoardType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(BoardType.DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Board") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = type == BoardType.GROCERY, onCheckedChange = { type = if (it) BoardType.GROCERY else BoardType.DEFAULT })
                    Text("Grocery List Mode")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name, description, type) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun JoinBoardDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var boardId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Board") },
        text = {
            OutlinedTextField(
                value = boardId,
                onValueChange = { boardId = it },
                label = { Text("Board ID") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (boardId.isNotBlank()) onJoin(boardId) }, enabled = boardId.isNotBlank()) {
                Text("Join")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit,
    onProfileClick: () -> Unit,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    showCelebration: Boolean,
    onShowCelebrationChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onProfileClick) {
                    Text("Profile", modifier = Modifier.debounceClick(onProfileClick))
                }
                Divider()
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ThemeOptionRow("Follow system", currentTheme == AppTheme.SYSTEM) { onThemeChange(AppTheme.SYSTEM) }
                ThemeOptionRow("Light", currentTheme == AppTheme.LIGHT) { onThemeChange(AppTheme.LIGHT) }
                ThemeOptionRow("Dark", currentTheme == AppTheme.DARK) { onThemeChange(AppTheme.DARK) }
                ThemeOptionRow("Ocean", currentTheme == AppTheme.OCEAN) { onThemeChange(AppTheme.OCEAN) }
                ThemeOptionRow("Forest", currentTheme == AppTheme.FOREST) { onThemeChange(AppTheme.FOREST) }
                ThemeOptionRow("Sunset", currentTheme == AppTheme.SUNSET) { onThemeChange(AppTheme.SUNSET) }
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowCelebrationChange(!showCelebration) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show celebration", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showCelebration, onCheckedChange = onShowCelebrationChange)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun BoardCard(
    board: Board,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (board.type == BoardType.GROCERY) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Grocery List", modifier = Modifier.padding(end = 8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(board.name, style = MaterialTheme.typography.titleMedium)
                if (board.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(board.description, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tasks: ${board.taskCount}", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit board")
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = "Share board")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete board")
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
