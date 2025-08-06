package com.example.smsfrauddetector

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smsfrauddetector.database.SmsReport
import com.example.smsfrauddetector.database.SmsReportRepository
import com.example.smsfrauddetector.llm.LlmInferenceHelper
import com.example.smsfrauddetector.FraudAnalysisResult
import com.example.smsfrauddetector.llm.Model
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class FraudDetectionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val messageChannel = Channel<Intent>(Channel.UNLIMITED)
    private val llmMutex = Mutex()
    private val processingJobs = ConcurrentHashMap<String, Job>()


    private var llmInference: LlmInference? = null
    private var currentModel: Model? = null
    private var cleanupJob: Job? = null

    companion object {
        const val ACTION_CANCEL_PROCESSING = "com.example.smsfrauddetector.ACTION_CANCEL_PROCESSING"
        const val ACTION_START_PROCESSING = "com.example.smsfrauddetector.ACTION_START_PROCESSING"
        const val ACTION_STOP_ALL_PROCESSING = "com.example.smsfrauddetector.ACTION_STOP_ALL_PROCESSING"
        const val KEY_MESSAGE_BODY = "message_body"
        const val KEY_SENDER = "sender"
        const val KEY_TEMP_ID = "temp_id"
        const val KEY_REPORT_ID = "report_id"
        private const val IDLE_TIMEOUT_MS = 30_000L // 30 seconds
        private const val SERVICE_NOTIFICATION_ID = 1 // Static ID for the service's notification
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("FraudDetectionService", "Service creating.")

        val notificationService = NotificationService(applicationContext)
        val notification = notificationService.createForegroundServiceNotification()
        startForeground(SERVICE_NOTIFICATION_ID, notification)

        processMessages()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FraudDetectionService", "Service started with intent action: ${intent?.action}")
        intent?.let {
            when (it.action) {
                ACTION_STOP_ALL_PROCESSING -> {
                    cancelAllJobs()
                }
                ACTION_CANCEL_PROCESSING -> {
                    val reportId = it.getStringExtra(KEY_REPORT_ID)
                    if (reportId != null) {
                        cancelJob(reportId)
                    }
                }
                ACTION_START_PROCESSING -> {
                    val reportId = it.getStringExtra(KEY_REPORT_ID)
                    if (reportId != null) {
                        startJob(reportId)
                    }
                }
                else -> {
                    // This is a new message to process
                    messageChannel.trySend(it)
                }
            }
        }
        return START_STICKY
    }

    private fun cancelJob(reportId: String) {
        val job = processingJobs[reportId]
        if (job != null && job.isActive) {
            Log.d("FraudDetectionService", "Cancelling job for reportId: $reportId")
            job.cancel() // This will trigger the finally block in the job
        }
        // Update the database to reflect the stopped state
        serviceScope.launch {
            val repository = SmsReportRepository.getInstance(applicationContext)
            repository.updateProcessingStatus(reportId, "stopped")
        }
        // Also remove the "processing" notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reportId.hashCode())
    }

    private fun cancelAllJobs() {
        Log.d("FraudDetectionService", "Cancelling all processing jobs.")
        // Create a copy of the keys to avoid ConcurrentModificationException
        val reportIds = processingJobs.keys.toSet()
        for (reportId in reportIds) {
            cancelJob(reportId)
        }
        serviceScope.launch {
            val repository = SmsReportRepository.getInstance(applicationContext)
            repository.stopAllProcessing()
        }
    }

    private fun startJob(reportId: String) {
        serviceScope.launch {
            val repository = SmsReportRepository.getInstance(applicationContext)
            val report = repository.getReportById(reportId)
            if (report != null && report.processingStatus == "stopped") {
                Log.d("FraudDetectionService", "Restarting job for reportId: $reportId")
                // Reset the status to "processing"
                repository.updateProcessingStatus(reportId, "processing")

                // Create a new intent and send it to the channel to be picked up by the processor
                val intent = Intent().apply {
                    putExtra(KEY_REPORT_ID, report.id)
                    putExtra(KEY_MESSAGE_BODY, report.messageBody)
                    putExtra(KEY_SENDER, report.sender)
                }
                messageChannel.trySend(intent)
            }
        }
    }

    private fun processMessages() {
        serviceScope.launch {
            val notificationService = NotificationService(applicationContext)
            for (intent in messageChannel) {
                val reportId = intent.getStringExtra(KEY_REPORT_ID)
                if (reportId == null) {
                    Log.e("FraudDetectionService", "Intent is missing Report ID, cannot process.")
                    continue
                }

                val job = launch {
                    try {
                        val messageBody = intent.getStringExtra(KEY_MESSAGE_BODY)!!
                        val sender = intent.getStringExtra(KEY_SENDER)!!

                        // 1. Show a unique "processing" notification with a cancel action
                        notificationService.sendCheckingNotification(messageBody, sender, reportId)

                        llmMutex.withLock {
                            // This block ensures all LLM operations are serialized
                            // 2. Load model if needed, cancelling any pending cleanup
                            loadModelIfNeeded()

                            // 3. Perform fraud detection
                            val fraudDetector = FraudDetector()
                            val analysisResult = if (llmInference != null && currentModel != null) {
                                fraudDetector.analyzeMessage(applicationContext, currentModel!!, llmInference!!, messageBody, sender)
                            } else {
                                fraudDetector.analyzeMessageLegacy(messageBody, sender)
                            }

                            // 4. Update database
                            updateDatabase(reportId, analysisResult)

                            // 5. Update notification with result
                            updateNotification(messageBody, sender, reportId, analysisResult)

                            // 6. Schedule cleanup
                            scheduleCleanup()
                        }
                    } catch (e: CancellationException) {
                        Log.d("FraudDetectionService", "Job for reportId $reportId was cancelled.")
                        // The job is cancelled, no need to do anything else.
                    } catch (e: Exception) {
                        Log.e("FraudDetectionService", "Error processing message for reportId $reportId", e)
                    } finally {
                        // 7. Ensure the job is removed from the map when it's done
                        processingJobs.remove(reportId)
                    }
                }
                processingJobs[reportId] = job
            }
        }
    }

    private suspend fun loadModelIfNeeded() {
        cleanupJob?.cancel() // Cancel any pending cleanup
        if (llmInference != null) return

        val context = applicationContext
        val models = com.example.smsfrauddetector.llm.ModelRegistry.models
        val downloadedModels = models.filter { model ->
            val filePath = model.getPath(context)
            val file = java.io.File(filePath)
            file.exists() && file.length() == model.sizeInBytes
        }

        if (downloadedModels.isNotEmpty()) {
            val selectedModelName = SettingsManager.getSelectedModelName(context)
            val model = downloadedModels.find { it.name == selectedModelName } ?: downloadedModels.first()
            model.preProcess()
            Log.d("FraudDetectionService", "Loading model: ${model.name}")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(model.getPath(context))
                .setMaxTokens(model.configValues[com.example.smsfrauddetector.llm.ConfigKey.MAX_TOKENS.label] as Int)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            currentModel = model
            Log.d("FraudDetectionService", "Model loaded successfully")
        } else {
            Log.w("FraudDetectionService", "No valid LLM models found.")
        }
    }

    private fun scheduleCleanup() {
        cleanupJob?.cancel()
        cleanupJob = serviceScope.launch {
            delay(IDLE_TIMEOUT_MS)
            llmMutex.withLock {
                // Re-check if still idle before cleaning up
                if (processingJobs.isEmpty()) {
                    Log.d("FraudDetectionService", "Idle timeout reached. Cleaning up LLM.")
                    llmInference?.close()
                    llmInference = null
                    currentModel = null
                    stopSelf() // Stop the service if the queue is empty and we are idle
                } else {
                    Log.d("FraudDetectionService", "Cleanup cancelled, new jobs arrived.")
                }
            }
        }
    }

    private suspend fun updateDatabase(reportId: String, analysisResult: FraudAnalysisResult) {
        val repository = SmsReportRepository.getInstance(applicationContext)
        try {
            val smsReport = repository.getReportById(reportId)
            if (smsReport != null) {
                val updatedReport = smsReport.copy(
                    isFraud = analysisResult.isFraud,
                    fraudProbability = analysisResult.confidence,
                    suspiciousWords = SmsReport.createSuspiciousWordsJson(analysisResult.suspiciousWords),
                    llmReason = analysisResult.reasons.firstOrNull(),
                    processingStatus = "processed"
                )
                repository.updateReport(updatedReport)
            }
        } catch (e: Exception) {
            Log.e("FraudDetectionService", "Failed to update SMS report", e)
        }
    }

    private fun updateNotification(messageBody: String, sender: String, reportId: String, analysisResult: FraudAnalysisResult) {
        val notificationService = NotificationService(applicationContext)
        val notificationId = reportId.hashCode()
        if (analysisResult.isFraud) {
            notificationService.sendFraudWarningNotification(messageBody, sender, reportId, notificationId)
        } else {
            notificationService.sendSafeMessageNotification(messageBody, sender, reportId, notificationId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FraudDetectionService", "Service destroying.")
        serviceScope.cancel()
        llmInference?.close()
    }
}