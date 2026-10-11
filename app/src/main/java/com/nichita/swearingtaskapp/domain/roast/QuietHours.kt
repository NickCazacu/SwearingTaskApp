package com.nichita.swearingtaskapp.domain.roast

import java.time.LocalTime

/**
 * True if [time] falls into the quiet window [start, end). The window may cross midnight
 * (23:00–08:00). Equal bounds mean "no quiet hours".
 */
fun isQuietTime(time: LocalTime, start: LocalTime, end: LocalTime): Boolean = when {
    start == end -> false
    start < end -> time >= start && time < end
    else -> time >= start || time < end
}
