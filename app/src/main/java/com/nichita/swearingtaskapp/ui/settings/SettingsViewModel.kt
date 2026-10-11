package com.nichita.swearingtaskapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichita.swearingtaskapp.BuildConfig
import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.RoastSource
import com.nichita.swearingtaskapp.domain.model.UserSettings
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import com.nichita.swearingtaskapp.domain.usecase.RoastCheckUseCase
import com.nichita.swearingtaskapp.ui.common.todayFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val today: LocalDate? = null,
    val aiConfigured: Boolean = BuildConfig.ROAST_BASE_URL.isNotBlank() && BuildConfig.ROAST_APP_TOKEN.isNotBlank(),
    val showDebugTools: Boolean = BuildConfig.DEBUG,
    val debugRoastInProgress: Boolean = false,
) {
    val pausedToday: Boolean get() = today != null && settings.isPausedOn(today)
}

sealed interface SettingsEvent {
    data class DebugRoast(val text: String, val source: RoastSource, val notified: Boolean) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val roastCheck: RoastCheckUseCase,
    clock: Clock,
) : ViewModel() {

    private val debugRoastInProgress = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        todayFlow(clock),
        debugRoastInProgress,
    ) { settings, today, inProgress ->
        SettingsUiState(settings = settings, today = today, debugRoastInProgress = inProgress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    fun setBaseLevel(level: RoastLevel) {
        viewModelScope.launch { settingsRepository.setBaseLevel(level) }
    }

    fun setInterval(hours: Int) {
        viewModelScope.launch { settingsRepository.setCheckIntervalHours(hours) }
    }

    fun setQuietStart(time: LocalTime) {
        val end = uiState.value.settings.quietEnd
        viewModelScope.launch { settingsRepository.setQuietHours(time, end) }
    }

    fun setQuietEnd(time: LocalTime) {
        val start = uiState.value.settings.quietStart
        viewModelScope.launch { settingsRepository.setQuietHours(start, time) }
    }

    fun setUseAi(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseAi(enabled) }
    }

    fun setVulgarity(vulgarity: Vulgarity) {
        viewModelScope.launch { settingsRepository.setVulgarity(vulgarity) }
    }

    fun togglePauseToday() {
        val state = uiState.value
        val today = state.today ?: return
        viewModelScope.launch { settingsRepository.setPausedOn(if (state.pausedToday) null else today) }
    }

    fun roastNow() {
        if (debugRoastInProgress.value) return
        debugRoastInProgress.value = true
        viewModelScope.launch {
            try {
                val result = roastCheck.roastNow()
                _events.send(SettingsEvent.DebugRoast(result.roast.text, result.roast.source, result.notified))
            } finally {
                debugRoastInProgress.value = false
            }
        }
    }
}
