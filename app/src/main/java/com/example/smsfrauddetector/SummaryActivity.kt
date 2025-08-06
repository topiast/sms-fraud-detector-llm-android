package com.example.smsfrauddetector

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.example.smsfrauddetector.database.SmsReport
import com.example.smsfrauddetector.database.SmsReportRepository
import com.example.smsfrauddetector.ui.theme.SmsFraudDetectorTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SummaryActivity : ComponentActivity() {

    private lateinit var repository: SmsReportRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = SmsReportRepository.getInstance(this)

        val reportId = intent.getStringExtra("REPORT_ID")

        setContent {
            SmsFraudDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (reportId != null) {
                        SummaryScreen(
                            reportId = reportId,
                            repository = repository,
                            modifier = Modifier.padding(innerPadding),
                            onBackPressed = { finish() },
                            onReportMessage = { showReportToast() },
                            onStartProcessing = { startProcessing(reportId) },
                            onStopProcessing = { stopProcessing(reportId) }
                        )
                    } else {
                        ErrorScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
    
    private fun deleteReport(reportId: String) {
        lifecycleScope.launch {
            try {
                repository.deleteReport(reportId)
                Toast.makeText(this@SummaryActivity, "Report deleted", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@SummaryActivity, "Failed to delete report", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleFlag(reportId: String) {
        lifecycleScope.launch {
            try {
                val report = repository.getReportById(reportId)
                if (report != null) {
                    val newFlagStatus = !report.isManuallyFlagged
                    repository.updateManualFlag(reportId, newFlagStatus)
                    Toast.makeText(
                        this@SummaryActivity, 
                        if (newFlagStatus) "Message flagged" else "Flag removed", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SummaryActivity, "Failed to update flag", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showReportToast() {
        Toast.makeText(this, "Report functionality coming soon!", Toast.LENGTH_LONG).show()
    }
    private fun startProcessing(reportId: String) {
        val intent = Intent(this, FraudDetectionService::class.java).apply {
            action = FraudDetectionService.ACTION_START_PROCESSING
            putExtra(FraudDetectionService.KEY_REPORT_ID, reportId)
        }
        startService(intent)
        Toast.makeText(this, "Restarting analysis...", Toast.LENGTH_SHORT).show()
    }

    private fun stopProcessing(reportId: String) {
        val intent = Intent(this, FraudDetectionService::class.java).apply {
            action = FraudDetectionService.ACTION_CANCEL_PROCESSING
            putExtra(FraudDetectionService.KEY_REPORT_ID, reportId)
        }
        startService(intent)
        Toast.makeText(this, "Stopping analysis...", Toast.LENGTH_SHORT).show()
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    reportId: String,
    repository: SmsReportRepository,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    onReportMessage: () -> Unit,
    onStartProcessing: () -> Unit,
    onStopProcessing: () -> Unit
) {
    val report by repository.getReportByIdFlow(reportId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (report == null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Show processing controls if the message is not yet processed
            if (report!!.processingStatus != "processed") {
                ProcessingControlsCard(
                    status = report!!.processingStatus,
                    onStart = onStartProcessing,
                    onStop = onStopProcessing
                )
            }

            // Show results if the message has been processed
            if (report!!.processingStatus == "processed") {
                FraudResultCard(report = report!!)
            }

            // Always show the message content
            MessageContentCard(report = report!!)

            // Report Button Only
            ReportButtonSection(
                onReportMessage = onReportMessage
            )
            }
        }
    }
}

@Composable
fun FraudResultCard(report: SmsReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.isFraud) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (report.isFraud) "⚠️ POSSIBLE SCAM" else "✅ LIKELY SAFE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (report.isFraud) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                if (report.isManuallyFlagged) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = "🚩 FLAGGED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            
            Text(
                text = "Scam Probability: ${report.getFraudPercentage()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Show LLM explanation (reason)
            if (!report.llmReason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${report.llmReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LinearProgressIndicator(
                progress = { report.fraudProbability.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = if (report.isFraud)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Analyzed: ${report.getFormattedDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MessageContentCard(report: SmsReport) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Message Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "From: ${report.sender ?: "Unknown"}",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Divider()
            
            Text(
                text = "Message Content:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Highlighted message text
            val suspiciousWords = report.getSuspiciousWordsList()
            val highlightedText = buildAnnotatedString {
                val messageText = report.messageBody
                var lastIndex = 0
                
                suspiciousWords.forEach { word ->
                    val index = messageText.indexOf(word, lastIndex, ignoreCase = true)
                    if (index != -1) {
                        // Add text before the suspicious word
                        append(messageText.substring(lastIndex, index))
                        
                        // Add highlighted suspicious word
                        withStyle(
                            style = SpanStyle(
                                color = Color.Red,
                                background = Color.Red.copy(alpha = 0.2f),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(messageText.substring(index, index + word.length))
                        }
                        
                        lastIndex = index + word.length
                    }
                }
                
                // Add remaining text
                if (lastIndex < messageText.length) {
                    append(messageText.substring(lastIndex))
                }
            }
            
            Text(
                text = highlightedText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
            
            if (suspiciousWords.isNotEmpty()) {
                Text(
                    text = "Suspicious words detected: ${suspiciousWords.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ProcessingControlsCard(
    status: String,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (status) {
                "processing" -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Analysis in progress...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                        Text("Stop Processing")
                    }
                }
                "stopped" -> {
                    Text(
                        text = "Analysis Stopped",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Press 'Start' to resume analysis.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text("Start Processing")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportButtonSection(
    onReportMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onReportMessage,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Report")
            }
        }
    }
}

@Composable
fun ErrorScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "❌",
                fontSize = 48.sp
            )
            Text(
                text = "Report not found",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "The requested message report could not be loaded.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}