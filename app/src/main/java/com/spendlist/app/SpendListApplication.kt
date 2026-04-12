package com.spendlist.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.spendlist.app.worker.AutoRenewalWorker
import com.spendlist.app.worker.ExchangeRateSyncWorker
import com.spendlist.app.worker.RenewalReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SpendListApplication : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Manually initialize WorkManager with HiltWorkerFactory
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
        WorkManager.initialize(this, config)

        createNotificationChannel()
        scheduleWorkers()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Auto-renewal: run immediately on app start to update stale dates
        val immediateAutoRenewal = OneTimeWorkRequestBuilder<AutoRenewalWorker>().build()
        workManager.enqueueUniqueWork(
            "auto_renewal_immediate",
            ExistingWorkPolicy.REPLACE,
            immediateAutoRenewal
        )

        // Auto-renewal: daily periodic check
        val autoRenewalWork = PeriodicWorkRequestBuilder<AutoRenewalWorker>(
            1, TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            AutoRenewalWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            autoRenewalWork
        )

        // Daily renewal reminder check
        val reminderWork = PeriodicWorkRequestBuilder<RenewalReminderWorker>(
            1, TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            RenewalReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )

        // Daily exchange rate sync
        val rateSyncWork = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(
            1, TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            ExchangeRateSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            rateSyncWork
        )
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "renewal_reminders"
    }
}