package com.nichita.swearingtaskapp.domain.usecase

import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.RoastState
import com.nichita.swearingtaskapp.domain.model.Task
import com.nichita.swearingtaskapp.domain.model.UserSettings
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RoastDecisionTest {

    private val zone = ZoneId.of("Europe/Chisinau")
    private val today = LocalDate.of(2026, 10, 11)
    private fun at(h: Int, m: Int = 0): ZonedDateTime = ZonedDateTime.of(today, LocalTime.of(h, m), zone)

    private val settings = UserSettings(baseLevel = RoastLevel.SOFT, checkIntervalHours = 2)
    private val pending = Task(id = 1, title = "диплом", date = today, createdAt = at(9).toInstant())
    private val done = Task(
        id = 2,
        title = "спорт",
        date = today,
        isDone = true,
        createdAt = at(9).toInstant(),
        completedAt = at(10).toInstant(),
    )

    private fun decide(
        now: ZonedDateTime = at(14),
        tasks: List<Task> = listOf(pending, done),
        settings: UserSettings = this.settings,
        state: RoastState = RoastState(),
        notificationsAllowed: Boolean = true,
    ) = decideRoast(now, tasks, settings, state, notificationsAllowed)

    private fun skip(reason: SkipReason) = RoastDecision.Skip(reason)

    @Test
    fun `roasts when there are undone tasks`() {
        val decision = decide()
        assertTrue(decision is RoastDecision.Roast)
        decision as RoastDecision.Roast
        // Idle since 10:00 (last completion), now 14:00 -> 4 h -> SOFT + 1.
        assertEquals(RoastLevel.MEDIUM, decision.context.level)
        assertEquals(4, decision.context.idleHours)
        assertEquals(listOf("диплом"), decision.context.pendingTasks)
    }

    @Test
    fun `profanity setting reaches the context`() {
        val off = decide() as RoastDecision.Roast
        val on = decide(settings = settings.copy(vulgarity = Vulgarity.PERSONAL)) as RoastDecision.Roast
        assertEquals(Vulgarity.CLEAN, off.context.vulgarity)
        assertEquals(Vulgarity.PERSONAL, on.context.vulgarity)
    }

    @Test
    fun `silent without notification permission`() {
        assertEquals(skip(SkipReason.NOTIFICATIONS_DISABLED), decide(notificationsAllowed = false))
    }

    @Test
    fun `silent when paused today but not when paused yesterday`() {
        assertEquals(skip(SkipReason.PAUSED), decide(settings = settings.copy(pausedOn = today)))
        assertTrue(decide(settings = settings.copy(pausedOn = today.minusDays(1))) is RoastDecision.Roast)
    }

    @Test
    fun `silent during quiet hours`() {
        assertEquals(skip(SkipReason.QUIET_HOURS), decide(now = at(23, 30)))
        assertEquals(skip(SkipReason.QUIET_HOURS), decide(now = at(7, 59)))
    }

    @Test
    fun `silent without tasks`() {
        assertEquals(skip(SkipReason.NO_TASKS), decide(tasks = emptyList()))
    }

    @Test
    fun `at most one roast per interval`() {
        val recent = RoastState(lastRoastAt = at(13).toInstant())
        assertEquals(skip(SkipReason.TOO_SOON), decide(state = recent))

        // A run slightly earlier than a full interval still counts as the next interval.
        val almostInterval = RoastState(lastRoastAt = at(14).minus(Duration.ofMinutes(110)).toInstant())
        assertTrue(decide(state = almostInterval) is RoastDecision.Roast)
    }

    @Test
    fun `praises once per day when everything is done`() {
        val allDone = listOf(done)
        assertTrue(decide(tasks = allDone) is RoastDecision.Praise)
        assertEquals(
            skip(SkipReason.ALREADY_PRAISED),
            decide(tasks = allDone, state = RoastState(lastPraiseDate = today)),
        )
        assertTrue(
            decide(tasks = allDone, state = RoastState(lastPraiseDate = today.minusDays(1))) is RoastDecision.Praise,
        )
    }

    @Test
    fun `praise ignores the roast interval`() {
        val decision = decide(tasks = listOf(done), state = RoastState(lastRoastAt = at(13, 50).toInstant()))
        assertTrue(decision is RoastDecision.Praise)
    }
}
