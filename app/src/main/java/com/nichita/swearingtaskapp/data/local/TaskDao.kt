package com.nichita.swearingtaskapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt ASC, id ASC")
    fun observeByDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt ASC, id ASC")
    suspend fun getByDate(date: LocalDate): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET isDone = :done, completedAt = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Instant?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND isDone = 0")
    suspend fun countUndone(date: LocalDate): Int

    @Query("UPDATE tasks SET date = :to, createdAt = :now WHERE date = :from AND isDone = 0")
    suspend fun moveUndone(from: LocalDate, to: LocalDate, now: Instant): Int
}
