package com.spendlist.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendlist.app.domain.usecase.renewal.MarkExpiredSubscriptionsUseCase
import com.spendlist.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ExpirationCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val markExpiredSubscriptions: MarkExpiredSubscriptionsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val newlyExpired = markExpiredSubscriptions()
            if (newlyExpired.isNotEmpty()) {
                NotificationHelper.sendExpirationNotification(applicationContext, newlyExpired)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "expiration_check"
    }
}
