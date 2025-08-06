package com.example.smsfrauddetector.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sms_reports")
data class SmsReport(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String?,
    val messageBody: String,
    val isFraud: Boolean,
    val fraudProbability: Double,
    val suspiciousWords: String, // JSON array stored as string
    val llmReason: String? = null, // LLM explanation/reason
    val isManuallyFlagged: Boolean = false,
    val isDeleted: Boolean = false,
    val processingStatus: String = "processing" // "processing", "processed", or "stopped"
) {
    /**
     * Helper function to get suspicious words as a list
     */
    fun getSuspiciousWordsList(): List<String> {
        return if (suspiciousWords.isNotEmpty()) {
            try {
                com.google.gson.Gson().fromJson(suspiciousWords, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Helper function to get formatted timestamp
     */
    fun getFormattedDate(): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
    
    /**
     * Helper function to get fraud probability as percentage
     */
    fun getFraudPercentage(): Int {
        return (fraudProbability * 100).toInt()
    }
    
    companion object {
        /**
         * Helper function to create suspicious words JSON string from list
         */
        fun createSuspiciousWordsJson(words: List<String>): String {
            return com.google.gson.Gson().toJson(words)
        }
    }
}