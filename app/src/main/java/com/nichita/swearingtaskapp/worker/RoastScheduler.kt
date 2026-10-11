package com.nichita.swearingtaskapp.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoastScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** (Re)schedules the periodic check. UPDATE keeps the existing work and applies the new period. */
    fun schedule(intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<RoastWorker>(
            repeatInterval = intervalHours.toLong(),
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_MINUTES,
            flexTimeIntervalUnit = TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private companion object {
        const val WORK_NAME = "roast_check"

        /** Runs land in the last 15 minutes of each period, keeping checks evenly spaced. */
        const val FLEX_MINUTES = 15L
    }
}
