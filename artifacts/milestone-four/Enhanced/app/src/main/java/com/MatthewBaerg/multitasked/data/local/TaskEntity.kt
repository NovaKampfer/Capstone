package com.MatthewBaerg.multitasked.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_tasks",
    foreignKeys = [ForeignKey(
        entity = BoardEntity::class, parentColumns = ["id"], childColumns = ["boardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["boardId", "createdAt"])]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val title: String,
    val completed: Boolean,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    @ColumnInfo(defaultValue = "NULL") val dueDate: Long? = null,
    @ColumnInfo(defaultValue = "'NONE'") val priority: String = "NONE"
)
