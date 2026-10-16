package com.MatthewBaerg.multitasked.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_boards")
data class BoardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)

// Counts are derived by a JOIN, so they cannot drift from the task records.
data class BoardSummary(val id: String, val name: String, val taskCount: Long)
