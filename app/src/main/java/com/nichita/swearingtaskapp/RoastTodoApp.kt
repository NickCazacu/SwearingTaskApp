package com.nichita.swearingtaskapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nichita.swearingtaskapp.domain.repository.SettingsRepository
import com.nichita.swearingtaskapp.worker.AndroidRoastNotifier
import com.nichita.swearingtaskapp.worker.RoastScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RoastTodoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var roastScheduler: RoastScheduler

    @Inject
    lateinit var notifier: AndroidRoastNotifier

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
        // Keeps the periodic check in sync with the interval chosen in settings.
        appScope.launch {
            settingsRepository.settings
                .map { it.checkIntervalHours }
                .distinctUntilChanged()
                .collect(roastScheduler::schedule)
        }
    }
}
