package com.nichita.swearingtaskapp.domain.roast

import com.nichita.swearingtaskapp.domain.model.RoastContext
import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.Task
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import java.time.Instant

/** Builds the generation context, applying escalation to [baseLevel]. */
fun buildRoastContext(
    todayTasks: List<Task>,
    baseLevel: RoastLevel,
    now: Instant,
    startOfDay: Instant,
    vulgarity: Vulgarity = Vulgarity.CLEAN,
): RoastContext {
    val idle = idleDuration(todayTasks, now, startOfDay)
    val (done, pending) = todayTasks.partition { it.isDone }
    return RoastContext(
        level = effectiveRoastLevel(baseLevel, idle),
        pendingTasks = pending.map { it.title },
        doneTasks = done.map { it.title },
        idleHours = idle.toHours().toInt(),
        doneCount = done.size,
        totalCount = todayTasks.size,
        vulgarity = vulgarity,
    )
}
