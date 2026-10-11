package com.nichita.swearingtaskapp.domain.roast

import com.nichita.swearingtaskapp.domain.model.Task
import java.time.Duration
import java.time.Instant

/**
 * Moment the user became idle: the latest completion today, or, if nothing was completed today,
 * the creation of the day's first task. Never earlier than [startOfDay]. Null when there are no
 * tasks.
 */
fun idleSince(todayTasks: List<Task>, startOfDay: Instant): Instant? {
    if (todayTasks.isEmpty()) return null
    val lastCompletion = todayTasks
        .mapNotNull { task -> task.completedAt?.takeIf { task.isDone && it >= startOfDay } }
        .maxOrNull()
    val anchor = lastCompletion ?: todayTasks.minOf { it.createdAt }
    return maxOf(anchor, startOfDay)
}

fun idleDuration(todayTasks: List<Task>, now: Instant, startOfDay: Instant): Duration {
    val since = idleSince(todayTasks, startOfDay) ?: return Duration.ZERO
    val idle = Duration.between(since, now)
    return if (idle.isNegative) Duration.ZERO else idle
}
