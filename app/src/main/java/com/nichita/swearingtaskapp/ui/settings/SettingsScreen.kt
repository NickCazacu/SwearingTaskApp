package com.nichita.swearingtaskapp.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichita.swearingtaskapp.R
import com.nichita.swearingtaskapp.domain.model.RoastLevel
import com.nichita.swearingtaskapp.domain.model.RoastSource
import com.nichita.swearingtaskapp.domain.model.UserSettings
import com.nichita.swearingtaskapp.domain.model.Vulgarity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@StringRes
fun RoastLevel.labelRes(): Int = when (this) {
    RoastLevel.SOFT -> R.string.level_soft
    RoastLevel.MEDIUM -> R.string.level_medium
    RoastLevel.HARD -> R.string.level_hard
}

@StringRes
fun RoastLevel.descriptionRes(): Int = when (this) {
    RoastLevel.SOFT -> R.string.level_soft_desc
    RoastLevel.MEDIUM -> R.string.level_medium_desc
    RoastLevel.HARD -> R.string.level_hard_desc
}

@StringRes
private fun Vulgarity.labelRes(): Int = when (this) {
    Vulgarity.CLEAN -> R.string.vulgarity_clean
    Vulgarity.PROFANITY -> R.string.vulgarity_profanity
    Vulgarity.PERSONAL -> R.string.vulgarity_personal
}

@StringRes
private fun Vulgarity.descriptionRes(): Int = when (this) {
    Vulgarity.CLEAN -> R.string.vulgarity_clean_desc
    Vulgarity.PROFANITY -> R.string.vulgarity_profanity_desc
    Vulgarity.PERSONAL -> R.string.vulgarity_personal_desc
}

private enum class QuietEdge { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val snackbarHostState = remember { SnackbarHostState() }
    var editingQuietEdge by rememberSaveable { mutableStateOf<QuietEdge?>(null) }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.DebugRoast -> {
                    val resultRes = when (event.source) {
                        RoastSource.AI -> R.string.settings_debug_result_ai
                        RoastSource.TEMPLATE -> R.string.settings_debug_result_template
                    }
                    val result = resources.getString(resultRes, event.text)
                    val message = if (event.notified) {
                        result
                    } else {
                        resources.getString(R.string.settings_debug_no_permission, result)
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Section(title = stringResource(R.string.settings_level_title)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RoastLevel.entries.forEachIndexed { index, level ->
                        SegmentedButton(
                            selected = settings.baseLevel == level,
                            onClick = { viewModel.setBaseLevel(level) },
                            shape = SegmentedButtonDefaults.itemShape(index, RoastLevel.entries.size),
                            label = { Text(stringResource(level.labelRes())) },
                        )
                    }
                }
                Text(
                    text = stringResource(settings.baseLevel.descriptionRes()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                HintText(stringResource(R.string.settings_level_hint))
            }

            HorizontalDivider()

            Section(title = stringResource(R.string.settings_interval_title)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UserSettings.INTERVAL_OPTIONS.forEachIndexed { index, hours ->
                        SegmentedButton(
                            selected = settings.checkIntervalHours == hours,
                            onClick = { viewModel.setInterval(hours) },
                            shape = SegmentedButtonDefaults.itemShape(index, UserSettings.INTERVAL_OPTIONS.size),
                            label = { Text(stringResource(R.string.settings_interval_value, hours)) },
                        )
                    }
                }
            }

            HorizontalDivider()

            Section(title = stringResource(R.string.settings_quiet_title)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { editingQuietEdge = QuietEdge.START },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_quiet_from, settings.quietStart.format(TimeFormatter)))
                    }
                    OutlinedButton(
                        onClick = { editingQuietEdge = QuietEdge.END },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_quiet_to, settings.quietEnd.format(TimeFormatter)))
                    }
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setUseAi(!settings.useAi) },
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_ai_title), style = MaterialTheme.typography.titleMedium)
                    HintText(stringResource(R.string.settings_ai_desc))
                    if (!state.aiConfigured) {
                        Text(
                            text = stringResource(R.string.settings_ai_not_configured),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Switch(checked = settings.useAi, onCheckedChange = viewModel::setUseAi)
            }

            HorizontalDivider()

            Section(title = stringResource(R.string.settings_vulgarity_title)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Vulgarity.entries.forEachIndexed { index, vulgarity ->
                        SegmentedButton(
                            selected = settings.vulgarity == vulgarity,
                            onClick = { viewModel.setVulgarity(vulgarity) },
                            shape = SegmentedButtonDefaults.itemShape(index, Vulgarity.entries.size),
                            label = { Text(stringResource(vulgarity.labelRes())) },
                        )
                    }
                }
                HintText(stringResource(settings.vulgarity.descriptionRes()))
            }

            HorizontalDivider()

            Section(title = stringResource(R.string.settings_pause_title)) {
                HintText(
                    stringResource(
                        if (state.pausedToday) R.string.settings_pause_active else R.string.settings_pause_inactive,
                    ),
                )
                if (state.pausedToday) {
                    OutlinedButton(onClick = viewModel::togglePauseToday, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_pause_resume))
                    }
                } else {
                    Button(onClick = viewModel::togglePauseToday, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_pause_today))
                    }
                }
            }

            if (state.showDebugTools) {
                HorizontalDivider()

                Section(title = stringResource(R.string.settings_debug_title)) {
                    OutlinedButton(
                        onClick = viewModel::roastNow,
                        enabled = !state.debugRoastInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (state.debugRoastInProgress) {
                                    R.string.settings_debug_generating
                                } else {
                                    R.string.settings_debug_roast_now
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    editingQuietEdge?.let { edge ->
        QuietTimeDialog(
            title = stringResource(
                if (edge == QuietEdge.START) R.string.settings_quiet_pick_start else R.string.settings_quiet_pick_end,
            ),
            initial = if (edge == QuietEdge.START) settings.quietStart else settings.quietEnd,
            onConfirm = { time ->
                if (edge == QuietEdge.START) viewModel.setQuietStart(time) else viewModel.setQuietEnd(time)
                editingQuietEdge = null
            },
            onDismiss = { editingQuietEdge = null },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietTimeDialog(
    title: String,
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
