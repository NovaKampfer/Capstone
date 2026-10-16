package com.MatthewBaerg.multitasked.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents a task board document in the "boards" collection.
 *
 * Firestore document shape:
 *  - id          (string, stored as field or just the document id)
 *  - name        (string)
 *  - description (string)
 *  - ownerId     (string)
 *  - memberIds   (array<string>)
 *  - taskCount   (number, incremented by Cloud / client)
 *  - type        (string, either "DEFAULT" or "GROCERY")
 */
data class Board(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val type: BoardType = BoardType.DEFAULT,

    // IMPORTANT: Firestore stores this as a number (Long).
    // Using Long here avoids mapping issues.
    @get:PropertyName("taskCount")
    @set:PropertyName("taskCount")
    var taskCount: Long = 0L
)

enum class BoardType {
    DEFAULT,
    GROCERY
}
