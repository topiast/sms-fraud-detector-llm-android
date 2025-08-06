package com.example.smsfrauddetector.database

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SmsReportRepository(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val smsReportDao = database.smsReportDao()
    
    // Get all reports
    fun getAllReports(): Flow<List<SmsReport>> = smsReportDao.getAllReports()
    
    // Get report by ID
    suspend fun getReportById(id: String): SmsReport? = smsReportDao.getReportById(id)

    // Get report by ID as a Flow
    fun getReportByIdFlow(id: String): Flow<SmsReport?> = smsReportDao.getReportByIdFlow(id)
    
    // Get filtered reports
    fun getFraudReports(): Flow<List<SmsReport>> = smsReportDao.getFraudReports()
    fun getSafeReports(): Flow<List<SmsReport>> = smsReportDao.getSafeReports()
    fun getFlaggedReports(): Flow<List<SmsReport>> = smsReportDao.getFlaggedReports()
    
    // Search reports
    fun searchReports(searchText: String): Flow<List<SmsReport>> = smsReportDao.searchReports(searchText)
    
    // Insert new report
    suspend fun insertReport(report: SmsReport) = smsReportDao.insertReport(report)
    
    // Update existing report
    suspend fun updateReport(report: SmsReport) = smsReportDao.updateReport(report)
    
    // Delete report (soft delete)
    suspend fun deleteReport(id: String) = smsReportDao.deleteReport(id)
    
    // Update manual flag
    suspend fun updateManualFlag(id: String, flagged: Boolean) = smsReportDao.updateManualFlag(id, flagged)

    // Update processing status
    suspend fun updateProcessingStatus(id: String, status: String) = smsReportDao.updateProcessingStatus(id, status)

    // Stop all processing
    suspend fun stopAllProcessing() = smsReportDao.stopAllProcessing()
    
    // Permanently delete marked reports
    suspend fun permanentlyDeleteMarkedReports() = smsReportDao.permanentlyDeleteMarkedReports()
    
    // Get counts
    suspend fun getTotalReportsCount(): Int = smsReportDao.getTotalReportsCount()
    suspend fun getFraudReportsCount(): Int = smsReportDao.getFraudReportsCount()
    suspend fun getFlaggedReportsCount(): Int = smsReportDao.getFlaggedReportsCount()
    
    companion object {
        @Volatile
        private var INSTANCE: SmsReportRepository? = null
        
        fun getInstance(context: Context): SmsReportRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SmsReportRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}