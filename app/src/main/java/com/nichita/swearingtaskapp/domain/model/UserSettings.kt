package com.nichita.swearingtaskapp.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class UserSettings(
    val baseLevel: RoastLevel = RoastLevel.MEDIUM,
    val checkIntervalHours: Int = DEFAULT_INTERVAL_HOURS,
    val quietStart: LocalTime = LocalTime.of(23, 0),
    val quietEnd: LocalTime = LocalTime.of(8, 0),
    val useAi: Boolean = true,
    /** Profanity / personal-insult mode. Opt-in, 18+. */
    val vulgarity: Vulgarity = Vulgarity.CLEAN,
    /** Roasting is paused for this date only ("Пауза на сегодня"). */
    val pausedOn: LocalDate? = null,
) {
    fun isPausedOn(date: LocalDate): Boolean = pausedOn == date

    companion object {
        const val DEFAULT_INTERVAL_HOURS = 2
        val INTERVAL_OPTIONS = listOf(1, 2, 3)
    }
}

/** Internal bookkeeping for notifications and the daily carry-over prompt. */
data class RoastState(
    val lastRoastAt: Instant? = null,
    val lastPraiseDate: LocalDate? = null,
    val lastCarryOverPromptDate: LocalDate? = null,
)
