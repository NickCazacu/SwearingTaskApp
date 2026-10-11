package com.nichita.swearingtaskapp.domain.usecase

import com.nichita.swearingtaskapp.domain.model.Roast
import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import com.nichita.swearingtaskapp.domain.repository.TaskRepository
import com.nichita.swearingtaskapp.domain.roast.RoastGenerator
import com.nichita.swearingtaskapp.domain.roast.RoastNotifier
import com.nichita.swearingtaskapp.domain.roast.buildRoastContext
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

data class ManualRoastResult(val roast: Roast, val notified: Boolean)

class RoastCheckUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val generator: RoastGenerator,
    private val notifier: RoastNotifier,
    private val clock: Clock,
) {
    /** Scheduled check from the worker. Respects pause, quiet hours and the interval limit. */
    suspend fun runScheduledCheck(): RoastDecision {
        val now = ZonedDateTime.now(clock)
        val decision = decideRoast(
            now = now,
            todayTasks = taskRepository.getTasks(now.toLocalDate()),
            settings = settingsRepository.settings.first(),
            state = settingsRepository.roastState.first(),
            notificationsAllowed = notifier.canNotify(),
        )
        when (decision) {
            is RoastDecision.Roast -> {
                notifier.showRoast(generator.roast(decision.context).text)
                settingsRepository.markRoasted(now.toInstant())
            }
            is RoastDecision.Praise -> {
                notifier.showPraise(generator.praise(decision.context).text)
                settingsRepository.markPraised(now.toLocalDate())
            }
            is RoastDecision.Skip -> Unit
        }
        return decision
    }

    /** Debug "roast me now": ignores every limit and does not affect the schedule. */
    suspend fun roastNow(): ManualRoastResult {
        val now = ZonedDateTime.now(clock)
        val today = now.toLocalDate()
        val settings = settingsRepository.settings.first()
        val context = buildRoastContext(
            todayTasks = taskRepository.getTasks(today),
            baseLevel = settings.baseLevel,
            now = now.toInstant(),
            startOfDay = today.atStartOfDay(now.zone).toInstant(),
            vulgarity = settings.vulgarity,
        )
        val roast = generator.roast(context)
        val canNotify = notifier.canNotify()
        if (canNotify) notifier.showRoast(roast.text)
        return ManualRoastResult(roast, notified = canNotify)
    }
}
