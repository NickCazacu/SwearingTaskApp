package com.nichita.swearingtaskapp.domain.repository

import com.nichita.swearingtaskapp.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observeTasks(date: LocalDate): Flow<List<Task>>

    suspend fun getTasks(date: LocalDate): List<Task>

    suspend fun addTask(title: String, date: LocalDate): Long

    suspend fun setDone(id: Long, done: Boolean)

    suspend fun delete(id: Long)

    /** Re-inserts a previously deleted task with its original id (undo). */
    suspend fun restore(task: Task)

    suspend fun countUndone(date: LocalDate): Int

    /**
     * Moves undone tasks from [from] to [to]. Moved tasks get a fresh createdAt: they are
     * re-planned for the new day, and idle time must not include the night.
     */
    suspend fun moveUndone(from: LocalDate, to: LocalDate): Int
}
