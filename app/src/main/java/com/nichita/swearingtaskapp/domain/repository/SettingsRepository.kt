package com.nichita.swearingtaskapp.domain.repository

import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.RoastState
import com.nichita.swearingtaskapp.domain.model.UserSettings
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

interface SettingsRepository {
    val settings: Flow<UserSettings>

    val roastState: Flow<RoastState>

    suspend fun setBaseLevel(level: RoastLevel)

    suspend fun setCheckIntervalHours(hours: Int)

    suspend fun setQuietHours(start: LocalTime, end: LocalTime)

    suspend fun setUseAi(enabled: Boolean)

    suspend fun setVulgarity(vulgarity: Vulgarity)

    suspend fun setPausedOn(date: LocalDate?)

    suspend fun markRoasted(at: Instant)

    suspend fun markPraised(date: LocalDate)

    suspend fun markCarryOverPrompted(date: LocalDate)
}
