package com.example.smsfrauddetector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * NotificationService manages all user notifications for SMS scanning.
 *
 * Notification Flow:
 * 1. When an SMS is received, sendCheckingNotification() is called to show an immediate "checking" notification.
 * 2. After fraud analysis, sendFraudWarningNotification() or sendSafeMessageNotification() is called with the same notification ID to update the notification in place.
 * 3. Notification IDs are based on a hash of message, sender, and timestamp for consistency.
 */
class NotificationService(private val context: Context) {

    companion object {
        private const val FRAUD_CHANNEL_ID = "fraud_detection_channel"
        private const val SAFE_CHANNEL_ID = "safe_message_channel"
        private const val SERVICE_CHANNEL_ID = "service_channel"
        private const val FRAUD_NOTIFICATION_ID = 1001
        private const val SAFE_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create fraud warning channel
            val fraudChannel = NotificationChannel(
                FRAUD_CHANNEL_ID,
                "Fraud Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for potential fraud messages"
                enableVibration(true)
                enableLights(true)
            }

            // Create safe message channel
            val safeChannel = NotificationChannel(
                SAFE_CHANNEL_ID,
                "Safe Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Confirmations that messages have been scanned and are safe"
                enableVibration(false)
                enableLights(false)
            }

            // Create service channel for the foreground service notification
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for the running fraud detection service"
                enableVibration(false)
                enableLights(false)
            }

            notificationManager.createNotificationChannel(fraudChannel)
            notificationManager.createNotificationChannel(safeChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
    
    fun sendFraudWarningNotification(
        messageBody: String,
        sender: String?,
        reportId: String,
        notificationId: Int = reportId.hashCode()
    ) {
        if (!hasNotificationPermission()) {
            return
        }

        // Create intent to open SummaryActivity with report ID
        val intent = Intent(context, SummaryActivity::class.java).apply {
            putExtra("REPORT_ID", reportId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FRAUD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ FRAUD ALERT")
            .setContentText("Potential fraud detected from ${sender ?: "Unknown"}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Potential fraud message detected:\n\nFrom: ${sender ?: "Unknown"}\nMessage: $messageBody\n\nTap to view details"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
        }
    }
    
    fun sendSafeMessageNotification(
        messageBody: String,
        sender: String?,
        reportId: String,
        notificationId: Int = reportId.hashCode()
    ) {
        if (!hasNotificationPermission()) {
            return
        }

        // Create intent to open SummaryActivity with report ID
        val intent = Intent(context, SummaryActivity::class.java).apply {
            putExtra("REPORT_ID", reportId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SAFE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✅ Message Scanned")
            .setContentText("Message from ${sender ?: "Unknown"} is safe")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Message successfully scanned and verified as safe:\n\nFrom: ${sender ?: "Unknown"}\nMessage: $messageBody\n\nTap to view details"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
        }
    }

    fun sendCheckingNotification(messageBody: String, sender: String?, reportId: String) {
        if (!hasNotificationPermission()) {
            return
        }

        // Create intent to open SummaryActivity with the report ID
        val intent = Intent(context, SummaryActivity::class.java).apply {
            putExtra("REPORT_ID", reportId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FRAUD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Checking message for fraud…")
            .setContentText("Analyzing message from ${sender ?: "Unknown"}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Checking message for fraud:\n\nFrom: ${sender ?: "Unknown"}\nMessage: $messageBody"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(reportId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
        }
    }

    fun createForegroundServiceNotification(): Notification {
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SMS Fraud Detector")
            .setContentText("Actively scanning incoming messages.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for Android 12 and below
        }
    }
}