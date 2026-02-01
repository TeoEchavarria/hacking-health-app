package com.samsung.android.health.sdk.sample.healthdiary.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.*
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.*
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.BlockEntity
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.RoutineDao
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.RoutineEntity

/**
 * Main Room database for the Health Diary application
 * Version 2: Added SensorBatch, UploadLog, MedicalDocument, TxAgentQuery, TxAgentResponse
 */
@Database(
    entities = [
        SensorDataEntity::class,
        SensorBatchEntity::class,
        UploadLogEntity::class,
        MedicalDocumentEntity::class,
        TxAgentQueryEntity::class,
        TxAgentResponseEntity::class,
        RoutineEntity::class,
        BlockEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun sensorBatchDao(): SensorBatchDao
    abstract fun uploadLogDao(): UploadLogDao
    abstract fun medicalDocumentDao(): MedicalDocumentDao
    abstract fun txAgentQueryDao(): TxAgentQueryDao
    abstract fun txAgentResponseDao(): TxAgentResponseDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to version 2
         * Adds new tables for comprehensive data management
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create sensor_batches table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sensor_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        sensorType TEXT NOT NULL,
                        dataJson TEXT NOT NULL,
                        sampleCount INTEGER NOT NULL,
                        uploaded INTEGER NOT NULL DEFAULT 0,
                        uploadedAt INTEGER,
                        receivedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create upload_logs table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS upload_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        endpoint TEXT NOT NULL,
                        responseCode INTEGER,
                        errorMessage TEXT,
                        dataSizeBytes INTEGER,
                        durationMs INTEGER
                    )
                """.trimIndent())
                
                // Create indices for upload_logs
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_upload_logs_entityType_entityId 
                    ON upload_logs(entityType, entityId)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_upload_logs_status 
                    ON upload_logs(status)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_upload_logs_timestamp 
                    ON upload_logs(timestamp)
                """.trimIndent())
                
                // Create medical_documents table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS medical_documents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        filename TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        uploadTimestamp INTEGER NOT NULL,
                        fileSize INTEGER NOT NULL,
                        mimeType TEXT NOT NULL,
                        description TEXT,
                        processed INTEGER NOT NULL DEFAULT 0,
                        processedAt INTEGER,
                        fileHash TEXT,
                        pageCount INTEGER,
                        tags TEXT
                    )
                """.trimIndent())
                
                // Create indices for medical_documents
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medical_documents_filename 
                    ON medical_documents(filename)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medical_documents_uploadTimestamp 
                    ON medical_documents(uploadTimestamp)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medical_documents_processed 
                    ON medical_documents(processed)
                """.trimIndent())
                
                // Create txagent_queries table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS txagent_queries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        queryText TEXT NOT NULL,
                        documentId INTEGER,
                        queryType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        sentAt INTEGER,
                        completedAt INTEGER,
                        errorMessage TEXT,
                        userId TEXT,
                        FOREIGN KEY(documentId) REFERENCES medical_documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for txagent_queries
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_documentId 
                    ON txagent_queries(documentId)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_status 
                    ON txagent_queries(status)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_queryType 
                    ON txagent_queries(queryType)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_timestamp 
                    ON txagent_queries(timestamp)
                """.trimIndent())
                
                // Create txagent_responses table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS txagent_responses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        queryId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        responseText TEXT NOT NULL,
                        metadata TEXT,
                        confidence REAL,
                        sources TEXT,
                        userRating INTEGER,
                        userFeedback TEXT,
                        processingTimeMs INTEGER,
                        modelVersion TEXT,
                        FOREIGN KEY(queryId) REFERENCES txagent_queries(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for txagent_responses
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_txagent_responses_queryId 
                    ON txagent_responses(queryId)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_responses_timestamp 
                    ON txagent_responses(timestamp)
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routineId TEXT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_routines_routineId ON routines(routineId)")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        blockId TEXT NOT NULL,
                        routineId TEXT NOT NULL,
                        exerciseName TEXT NOT NULL,
                        sets INTEGER NOT NULL,
                        targetWeight REAL NOT NULL,
                        targetReps INTEGER,
                        restSec INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        FOREIGN KEY(routineId) REFERENCES routines(routineId) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_blocks_routineId ON blocks(routineId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_blocks_blockId ON blocks(blockId)")
                // Seed one routine with 2 blocks for end-to-end validation
                val rid = "00000000-0000-0000-0000-000000000001"
                val bid1 = "00000000-0000-0000-0000-000000000002"
                val bid2 = "00000000-0000-0000-0000-000000000003"
                database.execSQL("INSERT OR IGNORE INTO routines (routineId, name) VALUES ('$rid', 'Sample Routine')")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid1', '$rid', 'Push-ups', 4, 0, 12, 60, 0)")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid2', '$rid', 'Squats', 4, 40, 10, 60, 1)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val rid = "00000000-0000-0000-0000-000000000001"
                val bid1 = "00000000-0000-0000-0000-000000000002"
                val bid2 = "00000000-0000-0000-0000-000000000003"
                database.execSQL("UPDATE routines SET name = 'Test Routine' WHERE routineId = '$rid'")
                database.execSQL("UPDATE blocks SET sets = 3, restSec = 30 WHERE routineId = '$rid'")
                database.execSQL("INSERT OR IGNORE INTO routines (routineId, name) VALUES ('$rid', 'Test Routine')")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid1', '$rid', 'Push-ups', 3, 0, 12, 30, 0)")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid2', '$rid', 'Squats', 3, 40, 10, 30, 1)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_diary_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration() // Only for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clear the database instance (useful for testing)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
