package com.example.smsfrauddetector.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SmsReport::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun smsReportDao(): SmsReportDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val migration_2_3 = object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL(
                            "ALTER TABLE sms_reports ADD COLUMN processingStatus TEXT NOT NULL DEFAULT 'not_processed'"
                        )
                    }
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_fraud_detector_database"
                )
                .addMigrations(migration_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}