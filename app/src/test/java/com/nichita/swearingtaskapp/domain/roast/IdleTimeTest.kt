package com.nichita.swearingtaskapp.domain.roast

import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class IdleTimeTest {

    private val today = LocalDate.of(2026, 10, 11)
    private val startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC)
    private fun at(h: Int, m: Int = 0) = today.atTime(LocalTime.of(h, m)).toInstant(ZoneOffset.UTC)

    private fun task(
        createdHour: Int,
        doneHour: Int? = null,
        title: String = "t$createdHour",
    ) = Task(
        title = title,
        date = today,
        isDone = doneHour != null,
        createdAt = at(createdHour),
        completedAt = doneHour?.let { at(it) },
    )

    @Test
    fun `no tasks means no idle time`() {
        assertNull(idleSince(emptyList(), startOfDay))
        assertEquals(Duration.ZERO, idleDuration(emptyList(), at(15), startOfDay))
    }

    @Test
    fun `counts from first task creation when nothing is done`() {
        val tasks = listOf(task(createdHour = 10), task(createdHour = 9), task(createdHour = 11))
        assertEquals(at(9), idleSince(tasks, startOfDay))
        assertEquals(Duration.ofHours(4), idleDuration(tasks, at(13), startOfDay))
    }

    @Test
    fun `counts from last completion today`() {
        val tasks = listOf(
            task(createdHour = 8, doneHour = 10),
            task(createdHour = 8, doneHour = 12),
            task(createdHour = 9),
        )
        assertEquals(at(12), idleSince(tasks, startOfDay))
        assertEquals(Duration.ofHours(3), idleDuration(tasks, at(15), startOfDay))
    }

    @Test
    fun `completion before today is ignored and anchor is clamped to start of day`() {
        val yesterdayEvening = startOfDay.minus(Duration.ofHours(2))
        val tasks = listOf(
            Task(title = "old", date = today, isDone = true, createdAt = yesterdayEvening, completedAt = yesterdayEvening),
            Task(title = "carried", date = today, createdAt = yesterdayEvening),
        )
        assertEquals(startOfDay, idleSince(tasks, startOfDay))
    }

    @Test
    fun `idle is never negative`() {
        val tasks = listOf(task(createdHour = 14))
        assertEquals(Duration.ZERO, idleDuration(tasks, at(13), startOfDay))
    }

    @Test
    fun `context applies escalation and counts`() {
        val tasks = listOf(
            task(createdHour = 8, doneHour = 9, title = "спорт"),
            task(createdHour = 8, title = "диплом"),
        )
        val context = buildRoastContext(tasks, RoastLevel.SOFT, now = at(15, 30), startOfDay = startOfDay)
        assertEquals(RoastLevel.HARD, context.level)
        assertEquals(6, context.idleHours)
        assertEquals(1, context.doneCount)
        assertEquals(2, context.totalCount)
        assertEquals(listOf("диплом"), context.pendingTasks)
        assertEquals(listOf("спорт"), context.doneTasks)
    }
}
