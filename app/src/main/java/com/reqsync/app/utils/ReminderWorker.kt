package com.reqsync.app.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager [Worker] that fires a local notification for a scheduled reminder.
 * Scheduled with [ReminderScheduler.schedule]; cancelled via [ReminderScheduler.cancel].
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "ReqSync Mission Alert"
        val message = inputData.getString(KEY_MESSAGE) ?: "You have pending requirements."
        NotificationHelper.showNotification(applicationContext, title, message)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "reminder_title"
        const val KEY_MESSAGE = "reminder_message"
        const val TAG_PREFIX = "reminder_"
    }
}

/**
 * Utility object to schedule and cancel [ReminderWorker] jobs via WorkManager.
 */
object ReminderScheduler {

    /**
     * Schedule a one-time notification at [delayMs] milliseconds from now.
     * @param reminderId Used as the unique work name tag so it can be cancelled.
     */
    fun schedule(
        context: Context,
        reminderId: Long,
        title: String,
        message: String,
        scheduledAtMillis: Long
    ) {
        val delayMs = scheduledAtMillis - System.currentTimeMillis()
        if (delayMs <= 0L) return // already past — skip

        val data = workDataOf(
            ReminderWorker.KEY_TITLE to title,
            ReminderWorker.KEY_MESSAGE to message
        )

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("${ReminderWorker.TAG_PREFIX}$reminderId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${ReminderWorker.TAG_PREFIX}$reminderId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Cancel a previously scheduled reminder. */
    fun cancel(context: Context, reminderId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("${ReminderWorker.TAG_PREFIX}$reminderId")
    }

    /** Cancel ALL active reminder workers. */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(ReminderWorker.TAG_PREFIX)
    }
}
