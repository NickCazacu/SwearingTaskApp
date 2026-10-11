package com.nichita.swearingtaskapp.data.repository

import com.nichita.swearingtaskapp.data.local.TaskDao
import com.nichita.swearingtaskapp.data.local.TaskEntity
import com.nichita.swearingtaskapp.data.local.toDomain
import com.nichita.swearingtaskapp.data.local.toEntity
import com.nichita.swearingtaskapp.domain.model.Task
import com.nichita.swearingtaskapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(date: LocalDate): Flow<List<Task>> =
        dao.observeByDate(date).map { list -> list.map(TaskEntity::toDomain) }

    override suspend fun getTasks(date: LocalDate): List<Task> =
        dao.getByDate(date).map(TaskEntity::toDomain)

    override suspend fun addTask(title: String, date: LocalDate): Long =
        dao.insert(
            TaskEntity(
                title = title.trim(),
                date = date,
                isDone = false,
                createdAt = clock.instant(),
                completedAt = null,
            )
        )

    override suspend fun setDone(id: Long, done: Boolean) =
        dao.setDone(id, done, if (done) clock.instant() else null)

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun restore(task: Task) {
        dao.insert(task.toEntity())
    }

    override suspend fun countUndone(date: LocalDate): Int = dao.countUndone(date)

    override suspend fun moveUndone(from: LocalDate, to: LocalDate): Int =
        dao.moveUndone(from, to, clock.instant())
}
