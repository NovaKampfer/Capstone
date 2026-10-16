package com.MatthewBaerg.multitasked.data.remote

import com.MatthewBaerg.multitasked.data.model.Board
import com.MatthewBaerg.multitasked.data.model.BoardType
import com.MatthewBaerg.multitasked.data.model.Priority
import com.MatthewBaerg.multitasked.data.model.TaskItem
import com.MatthewBaerg.multitasked.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirebaseDataSource(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {

    // region AUTH ----------------------------------------------

    private fun usersCollection() = db.collection("users")
    private fun errorsCollection() = db.collection("errors")

    suspend fun logError(functionName: String, errorMessage: String, boardId: String?) {
        try {
            val errorData = hashMapOf(
                "functionName" to functionName,
                "errorMessage" to errorMessage,
                "boardId" to boardId,
                "userId" to currentUserId(),
                "timestamp" to FieldValue.serverTimestamp()
            )
            errorsCollection().add(errorData).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to log error to Firestore")
        }
    }

    suspend fun register(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Registration succeeded but user is null")
        val newUser = User(id = user.uid, name = email.substringBefore('@'), email = email)
        usersCollection().document(user.uid).set(newUser).await()
        return user.uid
    }

    suspend fun login(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Login succeeded but user is null")

        // Ensure user document exists
        val userDoc = usersCollection().document(user.uid).get().await()
        if (!userDoc.exists()) {
            val newUser = User(id = user.uid, name = email.substringBefore('@'), email = email)
            usersCollection().document(user.uid).set(newUser).await()
        }

        return user.uid
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    fun authStateChanges(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid).isSuccess
        }

        auth.addAuthStateListener(listener)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun updateUserName(newName: String) {
        val userId = currentUserId() ?: throw IllegalStateException("User not logged in")
        usersCollection().document(userId).update("name", newName).await()
    }

    suspend fun getCurrentUser(): User? {
        val userId = currentUserId() ?: return null
        val snapshot = usersCollection().document(userId).get().await()
        return snapshot.toObject(User::class.java)
    }

    // endregion AUTH -------------------------------------------
    // region FCM TOKENS ----------------------------------------

    suspend fun addFcmToken(userId: String, token: String) {
        usersCollection().document(userId)
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .await()
    }

    suspend fun removeFcmToken(userId: String, token: String) {
        usersCollection().document(userId)
            .update("fcmTokens", FieldValue.arrayRemove(token))
            .await()
    }

    // endregion FCM TOKENS -------------------------------------
    // region BOARDS --------------------------------------------

    private fun boardsCollection() = db.collection("boards")
    private fun boardDoc(boardId: String) = boardsCollection().document(boardId)

    suspend fun createBoard(name: String, description: String, type: BoardType): Board {
        val userId = currentUserId()
            ?: throw IllegalStateException("User must be logged in to create a board")

        val docRef = boardsCollection().document()

        val board = Board(
            id = docRef.id,
            name = name,
            description = description,
            ownerId = userId,
            memberIds = listOf(userId),
            taskCount = 0L,
            type = type
        )

        docRef.set(board).await()
        return board
    }

    suspend fun updateBoard(boardId: String, name: String, description: String) {
        boardDoc(boardId).update(
            mapOf(
                "name" to name,
                "description" to description
            )
        ).await()
    }

    fun getBoardsForCurrentUser(): Flow<List<Board>> {
        val userId = currentUserId()
            ?: throw IllegalStateException("User must be logged in to load boards")

        return boardsCollection()
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val board = doc.toObject(Board::class.java)
                    board?.copy(id = if (board.id.isBlank()) doc.id else board.id)
                }
            }
    }

    suspend fun joinBoardById(boardId: String) {
        val userId = currentUserId()
            ?: throw IllegalStateException("User must be logged in to join a board")

        boardDoc(boardId)
            .update("memberIds", FieldValue.arrayUnion(userId))
            .await()
    }

    fun getBoard(boardId: String): Flow<Board?> {
        return boardDoc(boardId).snapshots().map { snap ->
            if (!snap.exists()) return@map null
            val board = snap.toObject(Board::class.java)
            board?.copy(id = if (board.id.isBlank()) snap.id else board.id)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBoardMembers(boardId: String): Flow<List<User>> {
        return boardDoc(boardId).snapshots().mapLatest { snap ->
            if (!snap.exists()) return@mapLatest emptyList<User>()
            val board = snap.toObject(Board::class.java)
            val memberIds = board?.memberIds ?: emptyList()
            if (memberIds.isEmpty()) return@mapLatest emptyList<User>()
            usersCollection().whereIn("id", memberIds).get().await().toObjects(User::class.java)
        }
    }

    suspend fun deleteBoard(boardId: String) {
        val tasksSnap = tasksCollection(boardId).get().await()
        for (taskDoc in tasksSnap.documents) {
            taskDoc.reference.delete().await()
        }
        boardDoc(boardId).delete().await()
    }

    // endregion BOARDS -----------------------------------------

    // region TASKS ---------------------------------------------

    private fun tasksCollection(boardId: String) =
        boardDoc(boardId).collection("tasks")

    fun getTasksForBoard(boardId: String): Flow<List<TaskItem>> {
        return tasksCollection(boardId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val task = doc.toObject(TaskItem::class.java)
                    task?.copy(
                        id = if (task.id.isBlank()) doc.id else task.id,
                        boardId = if (task.boardId.isBlank()) boardId else task.boardId
                    )
                }
            }
    }

    suspend fun addTask(boardId: String, title: String): TaskItem {
        val userId = currentUserId()
            ?: throw IllegalStateException("User must be logged in to add tasks")

        val docRef = tasksCollection(boardId).document()

        val now = System.currentTimeMillis()

        val task = TaskItem(
            id = docRef.id,
            boardId = boardId,
            title = title,
            isDone = false,
            createdBy = userId,
            createdAt = now
        )

        docRef.set(task).await()

        // Keep board's taskCount in sync
        boardDoc(boardId)
            .update("taskCount", FieldValue.increment(1))
            .await()

        return task
    }

    suspend fun toggleTaskDone(boardId: String, taskId: String, isDone: Boolean) {
        tasksCollection(boardId)
            .document(taskId)
            .update("isDone", isDone)
            .await()
    }

    suspend fun updateTaskPrice(boardId: String, taskId: String, price: Double) {
        tasksCollection(boardId)
            .document(taskId)
            .update("price", price)
            .await()
    }

    suspend fun updateTaskDetails(
        boardId: String,
        taskId: String,
        notes: String,
        dueDate: Long?,
        priority: Priority
    ) {
        tasksCollection(boardId)
            .document(taskId)
            .update(
                mapOf(
                    "notes" to notes,
                    "dueDate" to dueDate,
                    "priority" to priority
                )
            )
            .await()
    }

    suspend fun assignTask(boardId: String, taskId: String, userId: String) {
        tasksCollection(boardId)
            .document(taskId)
            .update("assignedTo", userId)
            .await()
    }

    suspend fun deleteTask(boardId: String, taskId: String) {
        tasksCollection(boardId)
            .document(taskId)
            .delete()
            .await()

        boardDoc(boardId)
            .update("taskCount", FieldValue.increment(-1))
            .await()
    }

    // endregion TASKS ------------------------------------------
}
