package com.spendlist.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendlist.app.domain.usecase.subscription.GetUpcomingRenewalsUseCase
import com.spendlist.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RenewalReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getUpcomingRenewals: GetUpcomingRenewalsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val upcoming = getUpcomingRenewals(withinDays = 3)
            for (subscription in upcoming) {
                NotificationHelper.sendRenewalNotification(applicationContext, subscription)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "renewal_reminder"
    }
}
