package com.example.smsfrauddetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.smsfrauddetector.database.SmsReport
import com.example.smsfrauddetector.SettingsManager
import com.example.smsfrauddetector.database.SmsReportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d(TAG, "SMS received")

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            // Concatenate all message bodies for multipart SMS
            val fullMessageBody = smsMessages.joinToString(separator = "") { it.messageBody }
            val sender = smsMessages.firstOrNull()?.originatingAddress

            Log.d(TAG, "Full message from $sender: $fullMessageBody")

            val tempId = "${sender ?: ""}_${fullMessageBody}_${System.currentTimeMillis()}"
            val reportId = java.util.UUID.randomUUID().toString()

            // --- Database Flow ---
            // 1. Insert a new SmsReport with status "not_processed" immediately after SMS is received.
            val smsReport = com.example.smsfrauddetector.database.SmsReport(
                id = reportId,
                sender = sender,
                messageBody = fullMessageBody,
                isFraud = false,
                fraudProbability = 0.0,
                suspiciousWords = "[]",
                llmReason = null,
                isManuallyFlagged = false,
                isDeleted = false,
                processingStatus = "processing"
            )
            val repository = com.example.smsfrauddetector.database.SmsReportRepository.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                repository.insertReport(smsReport)
            }

            // --- Service Flow ---
            // Start the FraudDetectionService to process the message
            val serviceIntent = Intent(context, FraudDetectionService::class.java).apply {
                putExtra(FraudDetectionService.KEY_MESSAGE_BODY, fullMessageBody)
                putExtra(FraudDetectionService.KEY_SENDER, sender)
                putExtra(FraudDetectionService.KEY_TEMP_ID, tempId)
                putExtra(FraudDetectionService.KEY_REPORT_ID, reportId)
            }
            context.startService(serviceIntent)
        }
    }
}