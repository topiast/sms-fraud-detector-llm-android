package com.example.smsfrauddetector

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smsfrauddetector.database.SmsReport
import com.example.smsfrauddetector.database.SmsReportRepository
import com.example.smsfrauddetector.ui.theme.SmsFraudDetectorTheme
import kotlinx.coroutines.launch
import java.util.UUID

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        setContent {
            SmsFraudDetectorTheme {
                val repository = SmsReportRepository.getInstance(this)
                val coroutineScope = rememberCoroutineScope()

                ShareScreen(
                    sharedText = sharedText,
                    onConfirm = {
                        if (sharedText != null) {
                            coroutineScope.launch {
                                val newReport = SmsReport(
                                    id = UUID.randomUUID().toString(),
                                    timestamp = System.currentTimeMillis(),
                                    sender = "Shared Text",
                                    messageBody = sharedText,
                                    isFraud = false,
                                    fraudProbability = 0.0,
                                    suspiciousWords = "[]",
                                    isManuallyFlagged = false,
                                    isDeleted = false,
                                    processingStatus = "processing"
                                )
                                repository.insertReport(newReport)

                                val serviceIntent = Intent(this@ShareReceiverActivity, FraudDetectionService::class.java).apply {
                                    putExtra(FraudDetectionService.KEY_REPORT_ID, newReport.id)
                                    putExtra(FraudDetectionService.KEY_MESSAGE_BODY, newReport.messageBody)
                                    putExtra(FraudDetectionService.KEY_SENDER, newReport.sender)
                                }
                                startService(serviceIntent)

                                val summaryIntent = Intent(this@ShareReceiverActivity, SummaryActivity::class.java).apply {
                                    putExtra("REPORT_ID", newReport.id)
                                }
                                startActivity(summaryIntent)
                                finish()
                            }
                        }
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ShareScreen(sharedText: String?, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val scrollState = androidx.compose.foundation.rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Text(
                    text = "Analyze shared message?",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = sharedText ?: "No text shared.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = onConfirm, enabled = sharedText != null) {
                        Text("Confirm")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}