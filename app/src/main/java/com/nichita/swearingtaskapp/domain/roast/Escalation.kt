package com.nichita.swearingtaskapp.domain.roast

import com.nichita.swearingtaskapp.domain.model.RoastLevel
import java.time.Duration

val ESCALATION_FIRST_STEP: Duration = Duration.ofHours(3)
val ESCALATION_SECOND_STEP: Duration = Duration.ofHours(6)

/**
 * Effective roast level: base + 1 step after 3 h without completed tasks, base + 2 steps
 * after 6 h, capped at [RoastLevel.HARD].
 */
fun effectiveRoastLevel(base: RoastLevel, idle: Duration): RoastLevel {
    val steps = when {
        idle >= ESCALATION_SECOND_STEP -> 2
        idle >= ESCALATION_FIRST_STEP -> 1
        else -> 0
    }
    return base.escalate(steps)
}
