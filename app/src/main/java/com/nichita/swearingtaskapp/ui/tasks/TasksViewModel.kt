package com.nichita.swearingtaskapp.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichita.swearingtaskapp.domain.model.Task
import com.nichita.swearingtaskapp.domain.repository.TaskRepository
import com.nichita.swearingtaskapp.domain.usecase.CarryOverUseCase
import com.nichita.swearingtaskapp.ui.common.todayFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class TasksUiState(
    val date: LocalDate? = null,
    val tasks: List<Task> = emptyList(),
    /** Undone tasks from yesterday to offer for carry-over; 0 hides the dialog. */
    val carryOverCount: Int = 0,
) {
    val doneCount: Int get() = tasks.count { it.isDone }
    val totalCount: Int get() = tasks.size
    val progress: Float get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
}

sealed interface TasksEvent {
    data class TaskDeleted(val task: Task) : TasksEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val carryOverUseCase: CarryOverUseCase,
    clock: Clock,
) : ViewModel() {

    private val today = todayFlow(clock)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val carryOverCount = MutableStateFlow(0)

    val uiState: StateFlow<TasksUiState> = combine(
        today.flatMapLatest { date -> taskRepository.observeTasks(date).map { date to it } },
        carryOverCount,
    ) { (date, tasks), carryOver ->
        TasksUiState(date = date, tasks = tasks, carryOverCount = carryOver)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    private val _events = Channel<TasksEvent>(Channel.BUFFERED)
    val events: Flow<TasksEvent> = _events.receiveAsFlow()

    init {
        // Checked on first launch of the day and again if the app stays open past midnight.
        viewModelScope.launch {
            today.collect { date -> carryOverCount.value = carryOverUseCase.pendingCount(date) }
        }
    }

    fun addTask(title: String) {
        val date = uiState.value.date ?: return
        if (title.isBlank()) return
        viewModelScope.launch { taskRepository.addTask(title, date) }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch { taskRepository.setDone(task.id, !task.isDone) }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            taskRepository.delete(task.id)
            _events.send(TasksEvent.TaskDeleted(task))
        }
    }

    fun undoDelete(task: Task) {
        viewModelScope.launch { taskRepository.restore(task) }
    }

    fun resolveCarryOver(move: Boolean) {
        val date = uiState.value.date ?: return
        carryOverCount.value = 0
        viewModelScope.launch { carryOverUseCase.resolve(date, move) }
    }
}
