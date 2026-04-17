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
import com.spendlist.app.worker.ExchangeRateSyncWorker
import com.spendlist.app.worker.ExpirationCheckWorker
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

        // Cancel legacy auto-renewal work from older app versions (no longer used).
        workManager.cancelUniqueWork("auto_renewal_immediate")
        workManager.cancelUniqueWork("auto_renewal")

        // Expiration check: run immediately on app start to catch newly-overdue subscriptions.
        val immediateExpirationCheck = OneTimeWorkRequestBuilder<ExpirationCheckWorker>().build()
        workManager.enqueueUniqueWork(
            "expiration_check_immediate",
            ExistingWorkPolicy.REPLACE,
            immediateExpirationCheck
        )

        // Expiration check: daily periodic run.
        val expirationCheckWork = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(
            1, TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            ExpirationCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            expirationCheckWork
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