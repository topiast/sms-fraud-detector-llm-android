package com.example.smsfrauddetector

/**
 * Enhanced data class to represent detailed fraud detection results
 * Used for creating SmsReport entities and summary screen display
 */
data class FraudAnalysisResult(
    val isFraud: Boolean,
    val confidence: Double,
    val suspiciousWords: List<String>,
    val reasons: List<String>,
    val sender: String?,
    val messageBody: String
)