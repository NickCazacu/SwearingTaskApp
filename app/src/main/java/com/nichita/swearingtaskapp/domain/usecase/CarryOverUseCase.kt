package com.nichita.swearingtaskapp.domain.usecase

import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import com.nichita.swearingtaskapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/** Offers to move yesterday's undone tasks to today, once per day. */
class CarryOverUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
) {
    /** Number of tasks to offer, or 0 if there is nothing to offer or we already asked today. */
    suspend fun pendingCount(today: LocalDate): Int {
        val state = settingsRepository.roastState.first()
        if (state.lastCarryOverPromptDate == today) return 0
        val count = taskRepository.countUndone(today.minusDays(1))
        if (count == 0) settingsRepository.markCarryOverPrompted(today)
        return count
    }

    suspend fun resolve(today: LocalDate, move: Boolean) {
        if (move) taskRepository.moveUndone(from = today.minusDays(1), to = today)
        settingsRepository.markCarryOverPrompted(today)
    }
}
