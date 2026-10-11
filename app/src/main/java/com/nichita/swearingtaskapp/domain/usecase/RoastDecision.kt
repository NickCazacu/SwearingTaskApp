package com.nichita.swearingtaskapp.domain.usecase

import com.nichita.swearingtaskapp.domain.model.RoastContext
import com.nichita.swearingtaskapp.domain.model.RoastState
import com.nichita.swearingtaskapp.domain.model.Task
import com.nichita.swearingtaskapp.domain.model.UserSettings
import com.nichita.swearingtaskapp.domain.roast.buildRoastContext
import com.nichita.swearingtaskapp.domain.roast.isQuietTime
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

sealed interface RoastDecision {
    data class Skip(val reason: SkipReason) : RoastDecision
    data class Roast(val context: RoastContext) : RoastDecision
    data class Praise(val context: RoastContext) : RoastDecision
}

enum class SkipReason {
    NOTIFICATIONS_DISABLED,
    PAUSED,
    QUIET_HOURS,
    NO_TASKS,
    ALREADY_PRAISED,
    TOO_SOON,
}

/**
 * Periodic work may fire somewhat earlier than a full interval after the previous run (flex
 * window), so roasts closer than `interval - tolerance` are considered "same interval".
 */
val ROAST_INTERVAL_TOLERANCE: Duration = Duration.ofMinutes(20)

/** Pure decision for a scheduled check: roast, praise or stay silent. */
fun decideRoast(
    now: ZonedDateTime,
    todayTasks: List<Task>,
    settings: UserSettings,
    state: RoastState,
    notificationsAllowed: Boolean,
): RoastDecision {
    val today = now.toLocalDate()
    val startOfDay: Instant = today.atStartOfDay(now.zone).toInstant()
    val instant = now.toInstant()

    if (!notificationsAllowed) return RoastDecision.Skip(SkipReason.NOTIFICATIONS_DISABLED)
    if (settings.isPausedOn(today)) return RoastDecision.Skip(SkipReason.PAUSED)
    if (isQuietTime(now.toLocalTime(), settings.quietStart, settings.quietEnd)) {
        return RoastDecision.Skip(SkipReason.QUIET_HOURS)
    }
    if (todayTasks.isEmpty()) return RoastDecision.Skip(SkipReason.NO_TASKS)

    val context = buildRoastContext(todayTasks, settings.baseLevel, instant, startOfDay, settings.vulgarity)

    if (todayTasks.all { it.isDone }) {
        return if (state.lastPraiseDate == today) {
            RoastDecision.Skip(SkipReason.ALREADY_PRAISED)
        } else {
            RoastDecision.Praise(context)
        }
    }

    val minGap = Duration.ofHours(settings.checkIntervalHours.toLong()).minus(ROAST_INTERVAL_TOLERANCE)
    val lastRoastAt = state.lastRoastAt
    if (lastRoastAt != null && Duration.between(lastRoastAt, instant) < minGap) {
        return RoastDecision.Skip(SkipReason.TOO_SOON)
    }
    return RoastDecision.Roast(context)
}
