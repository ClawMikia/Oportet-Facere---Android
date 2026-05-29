package com.reqsync.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reqsync.app.R
import com.reqsync.app.ui.MainActivity

const val CHANNEL_ID_REMINDERS = "reqsync_reminders"
const val EXTRA_NOTIFICATION_TITLE = "extra_title"
const val EXTRA_NOTIFICATION_MESSAGE = "extra_message"
const val EXTRA_REMINDER_ID = "extra_reminder_id"

// ─────────────────────────────────────────────────────────────────────────────
// NotificationReceiver — fires when an alarm triggers
// ─────────────────────────────────────────────────────────────────────────────
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: "ReqSync Mission Alert"
        val message = intent.getStringExtra(EXTRA_NOTIFICATION_MESSAGE) ?: "You have pending requirements."
        NotificationHelper.showNotification(context, title, message)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BootReceiver — reschedules reminders after device reboot
// ─────────────────────────────────────────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // WorkManager auto-reschedules; no action needed for periodic work
            // For exact alarms, would re-query DB and reschedule here
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NotificationHelper
// ─────────────────────────────────────────────────────────────────────────────
object NotificationHelper {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Mission Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Employment requirement deadline reminders"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String, id: Int = 1001) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }
}
