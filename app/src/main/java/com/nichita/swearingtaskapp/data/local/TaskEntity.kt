package com.nichita.swearingtaskapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nichita.swearingtaskapp.domain.model.Task
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "tasks", indices = [Index("date")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: LocalDate,
    val isDone: Boolean,
    val createdAt: Instant,
    val completedAt: Instant?,
)

fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    date = date,
    isDone = isDone,
    createdAt = createdAt,
    completedAt = completedAt,
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    date = date,
    isDone = isDone,
    createdAt = createdAt,
    completedAt = completedAt,
)
