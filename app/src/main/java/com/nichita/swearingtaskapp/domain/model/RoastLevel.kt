package com.nichita.swearingtaskapp.domain.model

enum class RoastLevel {
    SOFT,
    MEDIUM,
    HARD;

    /** Moves [steps] levels up, never above [HARD]. */
    fun escalate(steps: Int): RoastLevel {
        val target = (ordinal + steps.coerceAtLeast(0)).coerceAtMost(entries.lastIndex)
        return entries[target]
    }
}
