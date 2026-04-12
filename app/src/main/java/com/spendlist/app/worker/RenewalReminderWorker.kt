package com.spendlist.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendlist.app.data.datastore.UserPreferences
import com.spendlist.app.domain.usecase.subscription.GetUpcomingRenewalsUseCase
import com.spendlist.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RenewalReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getUpcomingRenewals: GetUpcomingRenewalsUseCase,
    private val userPreferences: UserPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val reminderEnabled = userPreferences.reminderEnabled.first()
            if (!reminderEnabled) return Result.success()

            val reminderDays = userPreferences.reminderDays.first()
            val maxDays = reminderDays.maxOrNull() ?: 3

            val upcoming = getUpcomingRenewals(withinDays = maxDays)
            for (subscription in upcoming) {
                val daysUntil = subscription.daysUntilRenewal().toInt()
                if (reminderDays.contains(daysUntil)) {
                    NotificationHelper.sendRenewalNotification(applicationContext, subscription)
                }
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
