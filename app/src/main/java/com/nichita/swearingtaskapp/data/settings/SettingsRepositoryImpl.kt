package com.nichita.swearingtaskapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.RoastState
import com.nichita.swearingtaskapp.domain.model.UserSettings
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val settings: Flow<UserSettings> = preferences.map { prefs ->
        val defaults = UserSettings()
        UserSettings(
            baseLevel = prefs[Keys.BASE_LEVEL]
                ?.let { name -> RoastLevel.entries.firstOrNull { it.name == name } }
                ?: defaults.baseLevel,
            checkIntervalHours = prefs[Keys.INTERVAL_HOURS]
                ?.takeIf { it in UserSettings.INTERVAL_OPTIONS }
                ?: defaults.checkIntervalHours,
            quietStart = prefs[Keys.QUIET_START_MINUTES]?.toLocalTime() ?: defaults.quietStart,
            quietEnd = prefs[Keys.QUIET_END_MINUTES]?.toLocalTime() ?: defaults.quietEnd,
            useAi = prefs[Keys.USE_AI] ?: defaults.useAi,
            vulgarity = prefs[Keys.VULGARITY]
                ?.let { name -> Vulgarity.entries.firstOrNull { it.name == name } }
                // Migration from the earlier on/off profanity switch.
                ?: if (prefs[Keys.ALLOW_PROFANITY] == true) Vulgarity.PROFANITY else defaults.vulgarity,
            pausedOn = prefs[Keys.PAUSED_ON]?.let(LocalDate::ofEpochDay),
        )
    }.distinctUntilChanged()

    override val roastState: Flow<RoastState> = preferences.map { prefs ->
        RoastState(
            lastRoastAt = prefs[Keys.LAST_ROAST_AT]?.let(Instant::ofEpochMilli),
            lastPraiseDate = prefs[Keys.LAST_PRAISE_DATE]?.let(LocalDate::ofEpochDay),
            lastCarryOverPromptDate = prefs[Keys.LAST_CARRY_OVER_DATE]?.let(LocalDate::ofEpochDay),
        )
    }.distinctUntilChanged()

    override suspend fun setBaseLevel(level: RoastLevel) {
        dataStore.edit { it[Keys.BASE_LEVEL] = level.name }
    }

    override suspend fun setCheckIntervalHours(hours: Int) {
        require(hours in UserSettings.INTERVAL_OPTIONS) { "Unsupported interval: $hours" }
        dataStore.edit { it[Keys.INTERVAL_HOURS] = hours }
    }

    override suspend fun setQuietHours(start: LocalTime, end: LocalTime) {
        dataStore.edit {
            it[Keys.QUIET_START_MINUTES] = start.toMinutes()
            it[Keys.QUIET_END_MINUTES] = end.toMinutes()
        }
    }

    override suspend fun setUseAi(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_AI] = enabled }
    }

    override suspend fun setVulgarity(vulgarity: Vulgarity) {
        dataStore.edit {
            it[Keys.VULGARITY] = vulgarity.name
            it.remove(Keys.ALLOW_PROFANITY)
        }
    }

    override suspend fun setPausedOn(date: LocalDate?) {
        dataStore.edit {
            if (date == null) it.remove(Keys.PAUSED_ON) else it[Keys.PAUSED_ON] = date.toEpochDay()
        }
    }

    override suspend fun markRoasted(at: Instant) {
        dataStore.edit { it[Keys.LAST_ROAST_AT] = at.toEpochMilli() }
    }

    override suspend fun markPraised(date: LocalDate) {
        dataStore.edit { it[Keys.LAST_PRAISE_DATE] = date.toEpochDay() }
    }

    override suspend fun markCarryOverPrompted(date: LocalDate) {
        dataStore.edit { it[Keys.LAST_CARRY_OVER_DATE] = date.toEpochDay() }
    }

    private fun LocalTime.toMinutes(): Int = hour * 60 + minute

    private fun Int.toLocalTime(): LocalTime = LocalTime.of((this / 60) % 24, this % 60)

    private object Keys {
        val BASE_LEVEL = stringPreferencesKey("base_level")
        val INTERVAL_HOURS = intPreferencesKey("check_interval_hours")
        val QUIET_START_MINUTES = intPreferencesKey("quiet_start_minutes")
        val QUIET_END_MINUTES = intPreferencesKey("quiet_end_minutes")
        val USE_AI = booleanPreferencesKey("use_ai")
        val VULGARITY = stringPreferencesKey("vulgarity")
        val ALLOW_PROFANITY = booleanPreferencesKey("allow_profanity")
        val PAUSED_ON = longPreferencesKey("paused_on_epoch_day")
        val LAST_ROAST_AT = longPreferencesKey("last_roast_at")
        val LAST_PRAISE_DATE = longPreferencesKey("last_praise_epoch_day")
        val LAST_CARRY_OVER_DATE = longPreferencesKey("last_carry_over_epoch_day")
    }
}
