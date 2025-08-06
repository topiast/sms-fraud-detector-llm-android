package com.example.smsfrauddetector.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsReportDao {
    
    @Query("SELECT * FROM sms_reports WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<SmsReport>>
    
    @Query("SELECT * FROM sms_reports WHERE id = :id AND isDeleted = 0")
    suspend fun getReportById(id: String): SmsReport?

    @Query("SELECT * FROM sms_reports WHERE id = :id AND isDeleted = 0")
    fun getReportByIdFlow(id: String): Flow<SmsReport?>
    
    @Query("SELECT * FROM sms_reports WHERE isFraud = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getFraudReports(): Flow<List<SmsReport>>
    
    @Query("SELECT * FROM sms_reports WHERE isFraud = 0 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getSafeReports(): Flow<List<SmsReport>>
    
    @Query("SELECT * FROM sms_reports WHERE isManuallyFlagged = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getFlaggedReports(): Flow<List<SmsReport>>
    
    @Query("SELECT * FROM sms_reports WHERE messageBody LIKE '%' || :searchText || '%' AND isDeleted = 0 ORDER BY timestamp DESC")
    fun searchReports(searchText: String): Flow<List<SmsReport>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SmsReport)
    
    @Update
    suspend fun updateReport(report: SmsReport)
    
    @Query("UPDATE sms_reports SET isDeleted = 1 WHERE id = :id")
    suspend fun deleteReport(id: String)
    
    @Query("UPDATE sms_reports SET isManuallyFlagged = :flagged WHERE id = :id")
    suspend fun updateManualFlag(id: String, flagged: Boolean)
    
    @Query("DELETE FROM sms_reports WHERE isDeleted = 1")
    suspend fun permanentlyDeleteMarkedReports()

    @Query("UPDATE sms_reports SET processingStatus = :status WHERE id = :id")
    suspend fun updateProcessingStatus(id: String, status: String)

    @Query("UPDATE sms_reports SET processingStatus = 'stopped' WHERE processingStatus = 'processing'")
    suspend fun stopAllProcessing()
    
    @Query("SELECT COUNT(*) FROM sms_reports WHERE isDeleted = 0")
    suspend fun getTotalReportsCount(): Int
    
    @Query("SELECT COUNT(*) FROM sms_reports WHERE isFraud = 1 AND isDeleted = 0")
    suspend fun getFraudReportsCount(): Int
    
    @Query("SELECT COUNT(*) FROM sms_reports WHERE isManuallyFlagged = 1 AND isDeleted = 0")
    suspend fun getFlaggedReportsCount(): Int
}