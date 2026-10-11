package com.nichita.swearingtaskapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nichita.swearingtaskapp.domain.usecase.RoastCheckUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class RoastWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val roastCheck: RoastCheckUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val decision = roastCheck.runScheduledCheck()
            Log.d(TAG, "Roast check: $decision")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Generation already falls back to templates; a failure here is not worth a retry
            // because the next periodic run comes anyway.
            Log.w(TAG, "Roast check failed", e)
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "RoastWorker"
    }
}
