package com.spendlist.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.spendlist.app.worker.RenewalReminderWorker

class TestReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TEST_REMINDER) {
            // Trigger the RenewalReminderWorker immediately
            val workRequest = OneTimeWorkRequestBuilder<RenewalReminderWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    companion object {
        const val ACTION_TEST_REMINDER = "com.spendlist.app.TEST_REMINDER"
    }
}